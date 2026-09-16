package com.buildtrack.ui;

import com.buildtrack.exceptions.BudgetExceededException;
import com.buildtrack.exceptions.DependencyException;
import com.buildtrack.exceptions.InvalidDeadlineException;
import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.exceptions.ResourceUnavailableException;
import com.buildtrack.model.BudgetSummary;
import com.buildtrack.model.BuildStep;
import com.buildtrack.model.Component;
import com.buildtrack.model.ComponentAvailability;
import com.buildtrack.model.Equipment;
import com.buildtrack.model.Expense;
import com.buildtrack.model.FeasibilityResult;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.InventoryItem;
import com.buildtrack.model.IoTProject;
import com.buildtrack.model.MechanicalProject;
import com.buildtrack.model.Project;
import com.buildtrack.model.ProjectComponent;
import com.buildtrack.model.PurchaseItem;
import com.buildtrack.model.TaskDependency;
import com.buildtrack.repository.ExpenseRepository;
import com.buildtrack.repository.InventoryRepository;
import com.buildtrack.repository.ProjectComponentRepository;
import com.buildtrack.service.BudgetService;
import com.buildtrack.service.BuildStepService;
import com.buildtrack.service.ComponentService;
import com.buildtrack.service.EquipmentService;
import com.buildtrack.service.FeasibilityService;
import com.buildtrack.service.InventoryService;
import com.buildtrack.service.ProjectService;
import com.buildtrack.threads.DeadlineAlert;
import com.buildtrack.threads.DeadlineMonitor;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BuildTrackCli {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Scanner scanner;
    private final PrintStream out;

    private final ProjectService projectService;
    private final BuildStepService buildStepService;
    private final ComponentService componentService;
    private final InventoryService inventoryService;
    private final BudgetService budgetService;
    private final FeasibilityService feasibilityService;
    private final EquipmentService equipmentService;
    private final ExpenseRepository expenseRepository;
    private final InventoryRepository inventoryRepository;
    private final ProjectComponentRepository projectComponentRepository;
    private final DeadlineMonitor deadlineMonitor;

    public BuildTrackCli() {
        this(System.in, System.out);
    }

    public BuildTrackCli(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;

        this.projectService = new ProjectService();
        this.buildStepService = new BuildStepService();
        this.componentService = new ComponentService();
        this.inventoryService = new InventoryService();
        this.budgetService = new BudgetService();
        this.feasibilityService = new FeasibilityService();
        this.equipmentService = new EquipmentService();
        this.expenseRepository = new ExpenseRepository();
        this.inventoryRepository = new InventoryRepository();
        this.projectComponentRepository = new ProjectComponentRepository();

        this.deadlineMonitor = new DeadlineMonitor(
                projectService, buildStepService, 60_000L, 3, this::onDeadlineAlert
        );
    }

    private void onDeadlineAlert(DeadlineAlert alert) {
        out.println("\n[ALERT] " + alert.message());
    }

    public void start() {
        printBanner();
        try {
            deadlineMonitor.start();
        } catch (Exception e) {
            out.println("Notice: Could not start background deadline monitor: " + e.getMessage());
        }

        boolean running = true;
        while (running) {
            try {
                printMainMenu();
                int choice = readInt("Select an option (1-10): ", 1, 10);
                switch (choice) {
                    case 1 -> showDashboard();
                    case 2 -> manageProjects();
                    case 3 -> manageBuildSteps();
                    case 4 -> manageComponents();
                    case 5 -> manageInventory();
                    case 6 -> manageExpenses();
                    case 7 -> analyzeFeasibility();
                    case 8 -> manageEquipment();
                    case 9 -> exportReports();
                    case 10 -> {
                        running = false;
                        out.println("\nShutting down BuildTrack. Goodbye!");
                    }
                    default -> out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                out.println("\n[Error] " + e.getMessage());
            }
        }

        try {
            deadlineMonitor.stop();
        } catch (Exception ignored) {
        }
    }

    private void printBanner() {
        out.println("=================================================");
        out.println("            BUILDTRACK SYSTEM ACTIVE             ");
        out.println("   Engineering Project Planning & Feasibility    ");
        out.println("=================================================");
    }

    private void printMainMenu() {
        out.println("\n================ MAIN MENU ================");
        out.println("1. Dashboard");
        out.println("2. Projects");
        out.println("3. Build Steps");
        out.println("4. Components");
        out.println("5. Inventory");
        out.println("6. Expenses");
        out.println("7. Feasibility Analysis");
        out.println("8. Equipment");
        out.println("9. Reports");
        out.println("10. Exit");
        out.println("===========================================");
    }

    // ==========================================
    // 1. DASHBOARD
    // ==========================================
    private void showDashboard() throws SQLException {
        out.println("\n-------------------------------------------");
        out.println("                DASHBOARD                  ");
        out.println("-------------------------------------------");

        List<Project> projects = projectService.findAllProjects();
        int totalProjects = projects.size();
        LocalDate today = LocalDate.now();

        int activeProjects = 0;
        int completedProjects = 0;
        int totalOverdueSteps = 0;
        int upcomingDeadlineSteps = 0;
        int projectsWithBudgetWarning = 0;
        int projectsWithShortages = 0;

        for (Project p : projects) {
            List<BuildStep> steps = buildStepService.findBuildSteps(p.getProjectId());
            double progress = BuildStepService.calculateProgress(steps);
            if (progress >= 100.0 && !steps.isEmpty()) {
                completedProjects++;
            } else {
                activeProjects++;
            }

            List<BuildStep> overdue = buildStepService.findOverdueSteps(p.getProjectId(), today);
            totalOverdueSteps += overdue.size();

            for (BuildStep s : steps) {
                if (s.getDeadline() != null && !s.getDeadline().isBefore(today)
                        && !s.getDeadline().isAfter(today.plusDays(7))
                        && s.getStatus() != BuildStep.Status.COMPLETED
                        && s.getStatus() != BuildStep.Status.CANCELLED) {
                    upcomingDeadlineSteps++;
                }
            }

            BudgetSummary summary = budgetService.getBudgetSummary(p);
            if (summary.status() != BudgetSummary.BudgetStatus.NORMAL) {
                projectsWithBudgetWarning++;
            }

            ComponentAvailability availability = inventoryService.getProjectComponentAvailability(p.getProjectId());
            if (!availability.missingQuantities().isEmpty()) {
                projectsWithShortages++;
            }
        }

        out.printf("Total Projects:              %d%n", totalProjects);
        out.printf("Active Projects:             %d%n", activeProjects);
        out.printf("Completed Projects:          %d%n", completedProjects);
        out.printf("Overdue Build Steps:         %d%n", totalOverdueSteps);
        out.printf("Upcoming Deadlines (<=7d):   %d%n", upcomingDeadlineSteps);
        out.printf("Projects with Budget Alerts: %d%n", projectsWithBudgetWarning);
        out.printf("Projects with Shortages:     %d%n", projectsWithShortages);
        int totalEquip = equipmentService.findAllEquipment().size();
        int availEquip = equipmentService.findAvailableEquipment().size();
        out.printf("Available Lab Equipment:     %d / %d unit(s)%n", availEquip, totalEquip);
        out.println("-------------------------------------------");
    }

    // ==========================================
    // 2. PROJECTS MANAGEMENT
    // ==========================================
    private void manageProjects() throws SQLException {
        boolean back = false;
        while (!back) {
            out.println("\n--- PROJECT MANAGEMENT ---");
            out.println("1. Create Project");
            out.println("2. List All Projects");
            out.println("3. View Project Details");
            out.println("4. Search Projects by Name");
            out.println("5. Update Project");
            out.println("6. Delete Project");
            out.println("7. Back to Main Menu");

            int choice = readInt("Select an option (1-7): ", 1, 7);
            switch (choice) {
                case 1 -> createProject();
                case 2 -> listProjects();
                case 3 -> viewProjectDetails();
                case 4 -> searchProjects();
                case 5 -> updateProject();
                case 6 -> deleteProject();
                case 7 -> back = true;
            }
        }
    }

    private void createProject() throws SQLException {
        out.println("\nCreate New Project:");
        out.println("Project Types: 1. Hardware | 2. Mechanical | 3. IoT");
        int typeChoice = readInt("Select Project Type (1-3): ", 1, 3);

        String name = readString("Project Name: ");
        String description = readString("Description: ");
        LocalDate startDate = readLocalDate("Start Date (YYYY-MM-DD): ");
        LocalDate deadline = readLocalDate("Deadline (YYYY-MM-DD): ");
        BigDecimal budget = readBigDecimal("Budget (₹): ");

        Project project = switch (typeChoice) {
            case 1 -> new HardwareProject(0, name, description, startDate, deadline, budget);
            case 2 -> new MechanicalProject(0, name, description, startDate, deadline, budget);
            case 3 -> new IoTProject(0, name, description, startDate, deadline, budget);
            default -> throw new IllegalArgumentException("Unknown type.");
        };

        try {
            int id = projectService.createProject(project);
            out.printf("✓ Project created successfully with ID: %d (%s)%n", id, project.getClass().getSimpleName());
        } catch (InvalidProjectException | InvalidDeadlineException e) {
            out.println("[Validation Error] " + e.getMessage());
        }
    }

    private void listProjects() throws SQLException {
        List<Project> projects = projectService.findAllProjects();
        if (projects.isEmpty()) {
            out.println("No projects found.");
            return;
        }

        out.println("\n-----------------------------------------------------------------------------------------");
        out.printf("%-6s %-30s %-12s %-12s %-12s %-12s%n", "ID", "Name", "Type", "Start Date", "Deadline", "Budget");
        out.println("-----------------------------------------------------------------------------------------");
        for (Project p : projects) {
            out.printf("%-6d %-30s %-12s %-12s %-12s ₹%-11s%n",
                    p.getProjectId(),
                    truncate(p.getName(), 30),
                    p.getClass().getSimpleName().replace("Project", ""),
                    p.getStartDate(),
                    p.getDeadline(),
                    p.getBudget());
        }
        out.println("-----------------------------------------------------------------------------------------");
    }

    private void viewProjectDetails() throws SQLException {
        int id = readInt("Enter Project ID: ");
        Project project = projectService.findProject(id);
        if (project == null) {
            out.println("Project not found with ID: " + id);
            return;
        }

        double progress = buildStepService.calculateProjectProgress(id);
        List<BuildStep> steps = buildStepService.findBuildSteps(id);

        out.println("\n================ PROJECT DETAILS ================");
        out.printf("ID:                   %d%n", project.getProjectId());
        out.printf("Name:                 %s%n", project.getName());
        out.printf("Type:                 %s%n", project.getClass().getSimpleName());
        out.printf("Description:          %s%n", project.getDescription());
        out.printf("Start Date:           %s%n", project.getStartDate());
        out.printf("Deadline:             %s%n", project.getDeadline());
        out.printf("Base Budget:          ₹%s%n", project.getBudget());
        out.printf("Estimated Cost:       ₹%s%n", project.calculateEstimatedCost());
        out.printf("Estimated Duration:   %d day(s)%n", project.calculateEstimatedDuration());
        out.printf("Total Build Steps:    %d%n", steps.size());
        out.printf("Calculated Progress:  %.1f%%%n", progress);
        List<Equipment> availEquip = equipmentService.findAvailableEquipment();
        out.printf("Available Lab Equip:  %d unit(s) ready%n", availEquip.size());
        out.println("=================================================");
    }

    private void searchProjects() throws SQLException {
        String keyword = readString("Enter keyword to search in project name: ").toLowerCase();
        List<Project> all = projectService.findAllProjects();
        List<Project> filtered = all.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword))
                .toList();

        if (filtered.isEmpty()) {
            out.println("No projects found matching '" + keyword + "'.");
            return;
        }

        out.println("\nMatching Projects:");
        for (Project p : filtered) {
            out.printf(" - [%d] %s (%s, Budget: ₹%s)%n",
                    p.getProjectId(), p.getName(), p.getClass().getSimpleName(), p.getBudget());
        }
    }

    private void updateProject() throws SQLException {
        int id = readInt("Enter Project ID to update: ");
        Project project = projectService.findProject(id);
        if (project == null) {
            out.println("Project not found with ID: " + id);
            return;
        }

        out.println("Updating Project '" + project.getName() + "'. Leave blank to keep current value.");
        String name = readOptionalString("New Name [" + project.getName() + "]: ", project.getName());
        String desc = readOptionalString("New Description [" + project.getDescription() + "]: ", project.getDescription());
        LocalDate start = readOptionalLocalDate("New Start Date [" + project.getStartDate() + "]: ", project.getStartDate());
        LocalDate deadline = readOptionalLocalDate("New Deadline [" + project.getDeadline() + "]: ", project.getDeadline());
        BigDecimal budget = readOptionalBigDecimal("New Budget [₹" + project.getBudget() + "]: ", project.getBudget());

        project.setName(name);
        project.setDescription(desc);
        project.setStartDate(start);
        project.setDeadline(deadline);
        project.setBudget(budget);

        try {
            projectService.updateProject(project);
            out.println("✓ Project updated successfully.");
        } catch (InvalidProjectException | InvalidDeadlineException e) {
            out.println("[Validation Error] " + e.getMessage());
        }
    }

    private void deleteProject() throws SQLException {
        int id = readInt("Enter Project ID to delete: ");
        Project project = projectService.findProject(id);
        if (project == null) {
            out.println("Project not found with ID: " + id);
            return;
        }

        String confirm = readString("Are you sure you want to delete '" + project.getName() + "' and all its steps? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm.trim())) {
            projectService.deleteProject(id);
            out.println("✓ Project deleted successfully.");
        } else {
            out.println("Deletion cancelled.");
        }
    }

    // ==========================================
    // 3. BUILD STEPS MANAGEMENT
    // ==========================================
    private void manageBuildSteps() throws SQLException {
        boolean back = false;
        while (!back) {
            out.println("\n--- BUILD STEP MANAGEMENT ---");
            out.println("1. Add Build Step");
            out.println("2. List Steps for Project");
            out.println("3. Update Step Status");
            out.println("4. Log Actual Hours");
            out.println("5. Add Task Dependency");
            out.println("6. View Task Dependencies");
            out.println("7. Show Overdue Steps for Project");
            out.println("8. Back to Main Menu");

            int choice = readInt("Select an option (1-8): ", 1, 8);
            switch (choice) {
                case 1 -> addBuildStep();
                case 2 -> listStepsForProject();
                case 3 -> updateStepStatus();
                case 4 -> logStepHours();
                case 5 -> addTaskDependency();
                case 6 -> viewTaskDependencies();
                case 7 -> showOverdueSteps();
                case 8 -> back = true;
            }
        }
    }

    private void addBuildStep() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        if (projectService.findProject(projectId) == null) {
            out.println("Project not found.");
            return;
        }

        String name = readString("Step Name: ");
        String description = readString("Description: ");
        double estHours = readDouble("Estimated Hours: ");
        LocalDate deadline = readOptionalLocalDate("Deadline (YYYY-MM-DD) [optional]: ", null);

        out.println("Priorities: 1. LOW | 2. MEDIUM | 3. HIGH | 4. CRITICAL");
        int priorityChoice = readInt("Select Priority (1-4): ", 1, 4);
        BuildStep.Priority priority = switch (priorityChoice) {
            case 1 -> BuildStep.Priority.LOW;
            case 2 -> BuildStep.Priority.MEDIUM;
            case 3 -> BuildStep.Priority.HIGH;
            case 4 -> BuildStep.Priority.CRITICAL;
            default -> BuildStep.Priority.MEDIUM;
        };

        BuildStep step = new BuildStep(
                0, projectId, name, description, estHours, 0.0,
                deadline, BuildStep.Status.PENDING, priority
        );

        try {
            int stepId = buildStepService.createBuildStep(step);
            out.printf("✓ Build Step added successfully with ID: %d%n", stepId);
        } catch (InvalidProjectException | InvalidDeadlineException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    private void listStepsForProject() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        List<BuildStep> steps = buildStepService.findBuildSteps(projectId);
        if (steps.isEmpty()) {
            out.println("No build steps found for project " + projectId);
            return;
        }

        out.println("\n-------------------------------------------------------------------------------------------------");
        out.printf("%-6s %-25s %-12s %-10s %-10s %-10s %-12s%n",
                "ID", "Name", "Status", "Priority", "Est(h)", "Act(h)", "Deadline");
        out.println("-------------------------------------------------------------------------------------------------");
        for (BuildStep s : steps) {
            out.printf("%-6d %-25s %-12s %-10s %-10.1f %-10.1f %-12s%n",
                    s.getStepId(),
                    truncate(s.getName(), 25),
                    s.getStatus(),
                    s.getPriority(),
                    s.getEstimatedHours(),
                    s.getActualHours(),
                    s.getDeadline() == null ? "None" : s.getDeadline().toString());
        }
        out.println("-------------------------------------------------------------------------------------------------");
    }

    private void updateStepStatus() throws SQLException {
        int stepId = readInt("Enter Build Step ID: ");
        out.println("Statuses: 1. PENDING | 2. IN_PROGRESS | 3. COMPLETED | 4. BLOCKED | 5. CANCELLED");
        int choice = readInt("Select Status (1-5): ", 1, 5);
        BuildStep.Status status = switch (choice) {
            case 1 -> BuildStep.Status.PENDING;
            case 2 -> BuildStep.Status.IN_PROGRESS;
            case 3 -> BuildStep.Status.COMPLETED;
            case 4 -> BuildStep.Status.BLOCKED;
            case 5 -> BuildStep.Status.CANCELLED;
            default -> BuildStep.Status.PENDING;
        };

        try {
            buildStepService.changeStatus(stepId, status);
            out.println("✓ Status updated to: " + status);
        } catch (DependencyException e) {
            out.println("[Dependency Error] " + e.getMessage());
        }
    }

    private void logStepHours() throws SQLException {
        int stepId = readInt("Enter Build Step ID: ");
        double hours = readDouble("Enter additional hours worked: ");
        if (hours < 0) {
            out.println("Hours cannot be negative.");
            return;
        }

        // Fetch step directly from project steps
        List<Project> projects = projectService.findAllProjects();
        BuildStep target = null;
        for (Project p : projects) {
            for (BuildStep s : buildStepService.findBuildSteps(p.getProjectId())) {
                if (s.getStepId() == stepId) {
                    target = s;
                    break;
                }
            }
            if (target != null) break;
        }

        if (target == null) {
            out.println("Build step not found.");
            return;
        }

        target.setActualHours(target.getActualHours() + hours);
        buildStepService.updateBuildStep(target);
        out.printf("✓ Updated actual hours for step '%s' to %.2f hours.%n", target.getName(), target.getActualHours());
    }

    private void addTaskDependency() throws SQLException {
        int stepId = readInt("Enter Dependent Step ID (the step that WAITS): ");
        int prereqId = readInt("Enter Prerequisite Step ID (the step that MUST COMPLETE FIRST): ");

        try {
            buildStepService.addDependency(stepId, prereqId);
            out.println("✓ Dependency successfully established: Step " + stepId + " depends on Step " + prereqId);
        } catch (DependencyException e) {
            out.println("[Dependency Error] " + e.getMessage());
        }
    }

    private void viewTaskDependencies() throws SQLException {
        int stepId = readInt("Enter Build Step ID: ");
        List<TaskDependency> deps = buildStepService.findDependencies(stepId);
        if (deps.isEmpty()) {
            out.println("Step " + stepId + " has no prerequisite dependencies.");
            return;
        }

        out.println("Prerequisites for Step " + stepId + ":");
        for (TaskDependency dep : deps) {
            out.println(" - Must wait for Step ID: " + dep.getPrerequisiteStepId());
        }
    }

    private void showOverdueSteps() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        List<BuildStep> overdue = buildStepService.findOverdueSteps(projectId, LocalDate.now());
        if (overdue.isEmpty()) {
            out.println("✓ No overdue steps for project " + projectId + ".");
            return;
        }

        out.println("\n⚠ OVERDUE STEPS:");
        for (BuildStep s : overdue) {
            out.printf(" - [%d] %s (Deadline: %s, Status: %s)%n",
                    s.getStepId(), s.getName(), s.getDeadline(), s.getStatus());
        }
    }

    // ==========================================
    // 4. COMPONENTS MANAGEMENT
    // ==========================================
    private void manageComponents() throws SQLException {
        boolean back = false;
        while (!back) {
            out.println("\n--- COMPONENT MANAGEMENT ---");
            out.println("1. Register Component");
            out.println("2. List All Components");
            out.println("3. Assign Component to Project (Requirement)");
            out.println("4. View Project Component Requirements (BOM)");
            out.println("5. Back to Main Menu");

            int choice = readInt("Select an option (1-5): ", 1, 5);
            switch (choice) {
                case 1 -> registerComponent();
                case 2 -> listComponents();
                case 3 -> assignComponentToProject();
                case 4 -> viewProjectRequirements();
                case 5 -> back = true;
            }
        }
    }

    private void registerComponent() throws SQLException {
        String name = readString("Component Name: ");
        String category = readString("Category (e.g. Microcontroller, Sensor, Motor, Passive): ");
        String unit = readString("Unit (e.g. piece, pack, meter): ");
        BigDecimal unitCost = readBigDecimal("Unit Cost (₹): ");

        Component component = new Component(0, name, category, unit, unitCost);
        try {
            int id = componentService.createComponent(component);
            out.printf("✓ Component registered successfully with ID: %d%n", id);
        } catch (InvalidProjectException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    private void listComponents() throws SQLException {
        List<Component> components = componentService.findAllComponents();
        if (components.isEmpty()) {
            out.println("No components registered.");
            return;
        }

        out.println("\n--------------------------------------------------------------------------------");
        out.printf("%-6s %-30s %-20s %-10s %-12s%n", "ID", "Name", "Category", "Unit", "Unit Cost");
        out.println("--------------------------------------------------------------------------------");
        for (Component c : components) {
            out.printf("%-6d %-30s %-20s %-10s ₹%-11s%n",
                    c.getComponentId(), truncate(c.getName(), 30), c.getCategory(), c.getUnit(), c.getUnitCost());
        }
        out.println("--------------------------------------------------------------------------------");
    }

    private void assignComponentToProject() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        int componentId = readInt("Enter Component ID: ");
        int quantity = readInt("Enter Required Quantity: ", 1, Integer.MAX_VALUE);

        ProjectComponent pc = new ProjectComponent(projectId, componentId, quantity);
        try {
            componentService.addProjectRequirement(pc);
            out.println("✓ Component requirement assigned to project.");
        } catch (InvalidProjectException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    private void viewProjectRequirements() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        List<ProjectComponent> reqs = projectComponentRepository.findByProjectId(projectId);
        if (reqs.isEmpty()) {
            out.println("No component requirements assigned to project " + projectId);
            return;
        }

        out.println("\n--- Bill of Materials (BOM) for Project " + projectId + " ---");
        out.printf("%-6s %-30s %-15s %-15s%n", "ID", "Component Name", "Qty Required", "Est. Total Cost");
        out.println("------------------------------------------------------------------");
        for (ProjectComponent pc : reqs) {
            Component c = componentService.findComponent(pc.getComponentId());
            String compName = c == null ? "Unknown" : c.getName();
            BigDecimal cost = c == null ? BigDecimal.ZERO : c.getUnitCost().multiply(BigDecimal.valueOf(pc.getQuantityRequired()));
            out.printf("%-6d %-30s %-15d ₹%-14s%n",
                    pc.getComponentId(), truncate(compName, 30), pc.getQuantityRequired(), cost);
        }
    }

    // ==========================================
    // 5. INVENTORY MANAGEMENT
    // ==========================================
    private void manageInventory() throws SQLException {
        boolean back = false;
        while (!back) {
            out.println("\n--- INVENTORY MANAGEMENT ---");
            out.println("1. View Current Inventory");
            out.println("2. Add / Update Component Stock");
            out.println("3. View Component Shortages for Project");
            out.println("4. Generate Smart Purchase List");
            out.println("5. Back to Main Menu");

            int choice = readInt("Select an option (1-5): ", 1, 5);
            switch (choice) {
                case 1 -> viewInventory();
                case 2 -> updateInventoryStock();
                case 3 -> viewShortages();
                case 4 -> generatePurchaseList();
                case 5 -> back = true;
            }
        }
    }

    private void viewInventory() throws SQLException {
        List<InventoryItem> items = inventoryRepository.findAll();
        if (items.isEmpty()) {
            out.println("Inventory is currently empty.");
            return;
        }

        out.println("\n-----------------------------------------------------------------");
        out.printf("%-12s %-12s %-30s %-15s%n", "Inventory ID", "Component ID", "Component Name", "Available Qty");
        out.println("-----------------------------------------------------------------");
        for (InventoryItem item : items) {
            Component c = componentService.findComponent(item.getComponentId());
            String name = c == null ? "Unknown" : c.getName();
            out.printf("%-12d %-12d %-30s %-15d%n",
                    item.getInventoryId(), item.getComponentId(), truncate(name, 30), item.getQuantityAvailable());
        }
        out.println("-----------------------------------------------------------------");
    }

    private void updateInventoryStock() throws SQLException {
        int componentId = readInt("Enter Component ID: ");
        if (componentService.findComponent(componentId) == null) {
            out.println("Component not found.");
            return;
        }

        int quantity = readInt("Enter available quantity in stock: ", 0, Integer.MAX_VALUE);
        try {
            inventoryService.addToInventory(componentId, quantity);
            out.println("✓ Inventory updated successfully for component " + componentId + ".");
        } catch (InvalidProjectException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    private void viewShortages() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        Map<Integer, Integer> shortages = inventoryService.calculateMissingQuantities(projectId);
        if (shortages.isEmpty()) {
            out.println("✓ All component requirements are fully available in inventory!");
            return;
        }

        out.println("\n⚠ COMPONENT SHORTAGES FOR PROJECT " + projectId + ":");
        for (Map.Entry<Integer, Integer> entry : shortages.entrySet()) {
            Component c = componentService.findComponent(entry.getKey());
            String name = c == null ? "Unknown" : c.getName();
            out.printf(" - Component [%d] '%s': missing %d unit(s)%n",
                    entry.getKey(), name, entry.getValue());
        }
    }

    private void generatePurchaseList() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        List<PurchaseItem> items = inventoryService.generatePurchaseList(projectId);
        if (items.isEmpty()) {
            out.println("✓ No purchases required. All components in stock.");
            return;
        }

        out.println("\n================ SMART PURCHASE LIST ================");
        out.printf("%-6s %-30s %-12s %-15s%n", "ID", "Component Name", "Qty to Buy", "Estimated Cost");
        out.println("-----------------------------------------------------");
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItem item : items) {
            out.printf("%-6d %-30s %-12d ₹%-14s%n",
                    item.componentId(), truncate(item.componentName(), 30), item.quantityToPurchase(), item.estimatedCost());
            total = total.add(item.estimatedCost());
        }
        out.println("-----------------------------------------------------");
        out.printf("TOTAL ESTIMATED PROCUREMENT COST: ₹%s%n", total);
        out.println("=====================================================");
    }

    // ==========================================
    // 6. EXPENSES & BUDGET MANAGEMENT
    // ==========================================
    private void manageExpenses() throws SQLException {
        boolean back = false;
        while (!back) {
            out.println("\n--- EXPENSES & BUDGET ---");
            out.println("1. Add Expense to Project");
            out.println("2. List Project Expenses");
            out.println("3. Show Project Budget Summary");
            out.println("4. Back to Main Menu");

            int choice = readInt("Select an option (1-4): ", 1, 4);
            switch (choice) {
                case 1 -> addExpense();
                case 2 -> listExpenses();
                case 3 -> showBudgetSummary();
                case 4 -> back = true;
            }
        }
    }

    private void addExpense() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        Project project = projectService.findProject(projectId);
        if (project == null) {
            out.println("Project not found.");
            return;
        }

        String desc = readString("Description: ");
        BigDecimal amount = readBigDecimal("Amount (₹): ");
        LocalDate date = readLocalDate("Expense Date (YYYY-MM-DD): ");
        String category = readString("Category (e.g. Hardware, Fabrication, Services, Miscellaneous): ");

        Expense expense = new Expense(0, projectId, desc, amount, date, category);
        int expenseId = expenseRepository.save(expense);
        out.printf("✓ Expense logged with ID: %d%n", expenseId);

        // Check budget threshold warning
        BudgetSummary summary = budgetService.getBudgetSummary(project);
        if (summary.status() != BudgetSummary.BudgetStatus.NORMAL) {
            out.printf("⚠ BUDGET ALERT: Status is %s (Utilization: %s%%)%n",
                    summary.status(), summary.utilizationPercent());
        }
    }

    private void listExpenses() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        List<Expense> expenses = expenseRepository.findByProjectId(projectId);
        if (expenses.isEmpty()) {
            out.println("No expenses logged for project " + projectId);
            return;
        }

        out.println("\n--------------------------------------------------------------------------------");
        out.printf("%-6s %-30s %-12s %-12s %-15s%n", "ID", "Description", "Amount", "Date", "Category");
        out.println("--------------------------------------------------------------------------------");
        BigDecimal sum = BigDecimal.ZERO;
        for (Expense e : expenses) {
            out.printf("%-6d %-30s ₹%-11s %-12s %-15s%n",
                    e.getExpenseId(), truncate(e.getDescription(), 30), e.getAmount(), e.getExpenseDate(), e.getCategory());
            sum = sum.add(e.getAmount());
        }
        out.println("--------------------------------------------------------------------------------");
        out.printf("TOTAL RECORDED EXPENSES: ₹%s%n", sum);
    }

    private void showBudgetSummary() throws SQLException {
        int projectId = readInt("Enter Project ID: ");
        Project project = projectService.findProject(projectId);
        if (project == null) {
            out.println("Project not found.");
            return;
        }

        BudgetSummary summary = budgetService.getBudgetSummary(project);

        out.println("\n================ PROJECT BUDGET SUMMARY ================");
        out.printf("Project:                    %s%n", project.getName());
        out.printf("Base Budget:                ₹%s%n", summary.projectBudget());
        out.printf("Estimated Components Cost:  ₹%s%n", summary.estimatedComponentCost());
        out.printf("Recorded Expenses:          ₹%s%n", summary.recordedExpenses());
        out.printf("Total Required Funds:       ₹%s%n", summary.totalRequired());
        out.printf("Remaining Budget:           ₹%s%n", summary.remainingBudget());
        out.printf("Utilization:                %s%%%n", summary.utilizationPercent());
        out.printf("Budget Status:              %s%n", summary.status());
        out.println("========================================================");

        if (summary.status() == BudgetSummary.BudgetStatus.WARNING) {
            out.println("⚠ WARNING: Project has reached over 75% budget utilization.");
        } else if (summary.status() == BudgetSummary.BudgetStatus.CRITICAL) {
            out.println("⚠ CRITICAL: Project has reached over 90% budget utilization.");
        } else if (summary.status() == BudgetSummary.BudgetStatus.OVER_BUDGET) {
            out.println("❌ OVER BUDGET: Required funds exceed the assigned budget!");
        }
    }

    // ==========================================
    // 7. FEASIBILITY ANALYSIS
    // ==========================================
    private void analyzeFeasibility() throws SQLException {
        int projectId = readInt("Enter Project ID to evaluate feasibility: ");
        Project project = projectService.findProject(projectId);
        if (project == null) {
            out.println("Project not found.");
            return;
        }

        FeasibilityResult r = feasibilityService.analyzeProject(projectId);

        out.println("\n=================================");
        out.println("       PROJECT FEASIBILITY       ");
        out.println("=================================");
        out.printf("Project: %s%n%n", r.projectName());

        out.println("TIME");
        out.printf("Required:  %.1f hours%n", r.totalEstimatedHours());
        out.printf("Available: %d days (%.1f work hours)%n", r.availableDays(), (double) (r.availableDays() * 8));
        out.printf("Status:    %s%n%n", formatStatus(r.timeStatus()));

        out.println("BUDGET");
        out.printf("Remaining: ₹%s%n", r.remainingBudget());
        out.printf("Status:    %s%n%n", formatStatus(r.budgetStatus()));

        out.println("COMPONENTS");
        out.printf("Required:  %d units%n", r.totalRequiredComponentQuantity());
        out.printf("Available: %d units%n", r.availableComponentQuantity());
        if (r.missingComponentQuantity() > 0) {
            out.printf("Status:    ⚠ %d MISSING%n%n", r.missingComponentQuantity());
        } else {
            out.println("Status:    ✓ SUFFICIENT\n");
        }

        out.println("DEPENDENCIES");
        if (r.blockedStepCount() > 0) {
            out.printf("Status:    ⚠ %d BLOCKED STEP(S)%n%n", r.blockedStepCount());
        } else {
            out.println("Status:    ✓ No blocked steps\n");
        }

        out.println("EQUIPMENT");
        List<Equipment> availEquip = equipmentService.findAvailableEquipment();
        List<Equipment> allEquip = equipmentService.findAllEquipment();
        out.printf("Available in Lab: %d / %d unit(s)%n", availEquip.size(), allEquip.size());
        if (!allEquip.isEmpty() && availEquip.isEmpty()) {
            out.println("Status:    ⚠ ALL LAB EQUIPMENT IN USE\n");
        } else {
            out.println("Status:    ✓ SUFFICIENT\n");
        }

        out.println("DEADLINE");
        if (r.overdueStepCount() > 0) {
            out.printf("Status:    ❌ %d OVERDUE STEP(S)%n", r.overdueStepCount());
        } else {
            out.printf("Status:    %s%n", formatStatus(r.deadlineStatus()));
        }

        out.println("---------------------------------");
        out.printf("HEALTH SCORE: %d/100%n", r.healthScore());
        out.printf("OVERALL:      %s%n", r.overallStatus());
        out.println("---------------------------------");

        out.println("RECOMMENDATIONS:");
        for (String rec : r.recommendations()) {
            out.println(" - " + rec);
        }
        out.println("=================================");
    }

    private String formatStatus(FeasibilityResult.FeasibilityStatus status) {
        return switch (status) {
            case ON_TRACK -> "✓ ON TRACK";
            case AT_RISK -> "⚠ AT RISK";
            case CRITICAL -> "❌ CRITICAL";
        };
    }

    // ==========================================
    // 8. EQUIPMENT MANAGEMENT
    // ==========================================
    private void manageEquipment() throws SQLException {
        boolean back = false;
        while (!back) {
            out.println("\n--- EQUIPMENT MANAGEMENT ---");
            out.println("1. List All Equipment");
            out.println("2. Add Equipment");
            out.println("3. Update Equipment Availability");
            out.println("4. Delete Equipment");
            out.println("5. Back to Main Menu");

            int choice = readInt("Select an option (1-5): ", 1, 5);
            switch (choice) {
                case 1 -> listEquipment();
                case 2 -> addEquipment();
                case 3 -> updateEquipmentAvailability();
                case 4 -> deleteEquipment();
                case 5 -> back = true;
            }
        }
    }

    private void listEquipment() throws SQLException {
        List<Equipment> equipmentList = equipmentService.findAllEquipment();
        if (equipmentList.isEmpty()) {
            out.println("No equipment registered in the system.");
            return;
        }

        out.println("\n------------------------------------------------------------------");
        out.printf("%-6s | %-24s | %-20s | %s%n", "ID", "Name", "Category", "Status");
        out.println("------------------------------------------------------------------");
        for (Equipment eq : equipmentList) {
            String status = eq.isAvailable() ? "AVAILABLE" : "IN USE";
            out.printf("%-6d | %-24s | %-20s | %s%n",
                    eq.getEquipmentId(),
                    truncate(eq.getName(), 24),
                    truncate(eq.getCategory(), 20),
                    status);
        }
        out.println("------------------------------------------------------------------");
    }

    private void addEquipment() throws SQLException {
        out.println("\nAdd New Equipment:");
        String name = readString("Equipment Name: ");
        if (name.isBlank()) {
            out.println("Error: Name cannot be blank.");
            return;
        }
        String category = readString("Category (e.g. Soldering, Measurement, 3D Printing): ");
        if (category.isBlank()) {
            out.println("Error: Category cannot be blank.");
            return;
        }
        String availInput = readString("Is it currently available? (Y/n): ");
        boolean available = !availInput.equalsIgnoreCase("n");

        Equipment equipment = new Equipment(0, name, category, available);
        int id = equipmentService.addEquipment(equipment);
        out.printf("✓ Equipment '%s' added successfully with ID %d!%n", name, id);
    }

    private void updateEquipmentAvailability() throws SQLException {
        int id = readInt("Enter Equipment ID to update availability: ");
        Equipment eq = equipmentService.findEquipment(id);
        if (eq == null) {
            out.println("Equipment not found with ID: " + id);
            return;
        }

        out.printf("Current status for '%s': %s%n", eq.getName(), eq.isAvailable() ? "AVAILABLE" : "IN USE");
        out.println("1. Mark AVAILABLE");
        out.println("2. Mark IN USE");
        int choice = readInt("Select status (1-2): ", 1, 2);
        boolean newStatus = (choice == 1);
        equipmentService.setEquipmentAvailability(id, newStatus);
        out.printf("✓ Equipment '%s' status updated to %s.%n", eq.getName(), newStatus ? "AVAILABLE" : "IN USE");
    }

    private void deleteEquipment() throws SQLException {
        int id = readInt("Enter Equipment ID to delete: ");
        Equipment eq = equipmentService.findEquipment(id);
        if (eq == null) {
            out.println("Equipment not found with ID: " + id);
            return;
        }

        String confirm = readString("Are you sure you want to delete '" + eq.getName() + "'? (y/N): ");
        if (confirm.equalsIgnoreCase("y")) {
            equipmentService.deleteEquipment(id);
            out.println("✓ Equipment deleted successfully.");
        } else {
            out.println("Deletion cancelled.");
        }
    }

    // ==========================================
    // 9. REPORTS
    // ==========================================
    private void exportReports() throws SQLException {
        int projectId = readInt("Enter Project ID for report export: ");
        Project project = projectService.findProject(projectId);
        if (project == null) {
            out.println("Project not found.");
            return;
        }

        out.println("\nReport Formats: 1. TXT | 2. CSV | 3. Both TXT & CSV");
        int formatChoice = readInt("Select Format (1-3): ", 1, 3);

        BudgetSummary budget = budgetService.getBudgetSummary(project);
        FeasibilityResult feasibility = feasibilityService.analyzeProject(projectId);
        List<BuildStep> steps = buildStepService.findBuildSteps(projectId);
        List<PurchaseItem> purchases = inventoryService.generatePurchaseList(projectId);
        List<Expense> expenses = expenseRepository.findByProjectId(projectId);

        try {
            if (formatChoice == 1 || formatChoice == 3) {
                Path txt = ReportExporter.exportToTxt(project, budget, feasibility, steps, purchases, expenses);
                out.println("✓ Exported TXT Report: " + txt.toAbsolutePath());
            }
            if (formatChoice == 2 || formatChoice == 3) {
                Path csv = ReportExporter.exportToCsv(project, steps, expenses);
                out.println("✓ Exported CSV Report: " + csv.toAbsolutePath());
            }
        } catch (Exception e) {
            out.println("[Export Error] Could not write report file: " + e.getMessage());
        }
    }

    // ==========================================
    // INPUT HELPERS
    // ==========================================
    private String readString(String prompt) {
        out.print(prompt);
        return scanner.nextLine().trim();
    }

    private String readOptionalString(String prompt, String defaultValue) {
        out.print(prompt);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    private int readInt(String prompt) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                out.println("Invalid integer. Please enter a valid whole number.");
            }
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            int val = readInt(prompt);
            if (val >= min && val <= max) {
                return val;
            }
            out.printf("Value out of range [%d - %d]. Try again.%n", min, max);
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                out.println("Invalid decimal number. Please try again.");
            }
        }
    }

    private BigDecimal readBigDecimal(String prompt) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                BigDecimal val = new BigDecimal(input);
                if (val.signum() < 0) {
                    out.println("Monetary value cannot be negative.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                out.println("Invalid monetary format (e.g. 1500.00). Try again.");
            }
        }
    }

    private BigDecimal readOptionalBigDecimal(String prompt, BigDecimal defaultValue) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return defaultValue;
            try {
                BigDecimal val = new BigDecimal(input);
                if (val.signum() < 0) {
                    out.println("Monetary value cannot be negative.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                out.println("Invalid monetary format. Try again.");
            }
        }
    }

    private LocalDate readLocalDate(String prompt) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input, DATE_FMT);
            } catch (DateTimeParseException e) {
                out.println("Invalid date format. Use YYYY-MM-DD (e.g. 2026-10-15).");
            }
        }
    }

    private LocalDate readOptionalLocalDate(String prompt, LocalDate defaultValue) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return defaultValue;
            try {
                return LocalDate.parse(input, DATE_FMT);
            } catch (DateTimeParseException e) {
                out.println("Invalid date format. Use YYYY-MM-DD.");
            }
        }
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }
}

