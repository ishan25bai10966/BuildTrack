package com.buildtrack.ui;

import com.buildtrack.model.BudgetSummary;
import com.buildtrack.model.BuildStep;
import com.buildtrack.model.Expense;
import com.buildtrack.model.FeasibilityResult;
import com.buildtrack.model.Project;
import com.buildtrack.model.PurchaseItem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportExporter {

    private static final String REPORTS_DIR = "reports";
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static Path exportToTxt(Project project,
                                   BudgetSummary budget,
                                   FeasibilityResult feasibility,
                                   List<BuildStep> steps,
                                   List<PurchaseItem> purchaseList,
                                   List<Expense> expenses) throws IOException {
        Path dir = Paths.get(REPORTS_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String filename = String.format("project_%d_%s.txt", project.getProjectId(),
                LocalDateTime.now().format(FILE_DATE_FMT));
        Path filePath = dir.resolve(filename);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write("================================================================================");
            writer.newLine();
            writer.write("                   BUILDTRACK PROJECT SUMMARY REPORT");
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.write("Project ID:   " + project.getProjectId());
            writer.newLine();
            writer.write("Name:         " + project.getName());
            writer.newLine();
            writer.write("Description:  " + project.getDescription());
            writer.newLine();
            writer.write("Start Date:   " + project.getStartDate());
            writer.newLine();
            writer.write("Deadline:     " + project.getDeadline());
            writer.newLine();
            writer.write("Budget:       " + project.getBudget());
            writer.newLine();
            writer.write("Est. Cost:    " + project.calculateEstimatedCost());
            writer.newLine();
            writer.write("Est. Duration:" + project.calculateEstimatedDuration() + " day(s)");
            writer.newLine();
            writer.newLine();

            if (feasibility != null) {
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();
                writer.write("FEASIBILITY ASSESSMENT");
                writer.newLine();
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();
                writer.write("Health Score:     " + feasibility.healthScore() + "/100");
                writer.newLine();
                writer.write("Overall Status:   " + feasibility.overallStatus());
                writer.newLine();
                writer.write("Time Status:      " + feasibility.timeStatus());
                writer.newLine();
                writer.write("Budget Status:    " + feasibility.budgetStatus());
                writer.newLine();
                writer.write("Component Status: " + feasibility.componentStatus());
                writer.newLine();
                writer.write("Dependency Status:" + feasibility.dependencyStatus());
                writer.newLine();
                writer.write("Deadline Status:  " + feasibility.deadlineStatus());
                writer.newLine();
                writer.write("Recommendations:");
                writer.newLine();
                for (String rec : feasibility.recommendations()) {
                    writer.write("  - " + rec);
                    writer.newLine();
                }
                writer.newLine();
            }

            if (budget != null) {
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();
                writer.write("BUDGET SUMMARY");
                writer.newLine();
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();
                writer.write("Project Budget:      " + budget.projectBudget());
                writer.newLine();
                writer.write("Component Cost:      " + budget.estimatedComponentCost());
                writer.newLine();
                writer.write("Recorded Expenses:   " + budget.recordedExpenses());
                writer.newLine();
                writer.write("Total Required:      " + budget.totalRequired());
                writer.newLine();
                writer.write("Remaining Budget:    " + budget.remainingBudget());
                writer.newLine();
                writer.write("Utilization:         " + budget.utilizationPercent() + "% (" + budget.status() + ")");
                writer.newLine();
                writer.newLine();
            }

            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();
            writer.write("BUILD STEPS (" + (steps == null ? 0 : steps.size()) + ")");
            writer.newLine();
            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();
            if (steps != null && !steps.isEmpty()) {
                writer.write(String.format("%-6s %-25s %-12s %-10s %-10s %-12s", "ID", "Name", "Status", "Est(h)", "Act(h)", "Deadline"));
                writer.newLine();
                for (BuildStep step : steps) {
                    writer.write(String.format("%-6d %-25s %-12s %-10.1f %-10.1f %-12s",
                            step.getStepId(),
                            truncate(step.getName(), 25),
                            step.getStatus(),
                            step.getEstimatedHours(),
                            step.getActualHours(),
                            step.getDeadline() == null ? "None" : step.getDeadline().toString()));
                    writer.newLine();
                }
            } else {
                writer.write("No build steps defined.");
                writer.newLine();
            }
            writer.newLine();

            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();
            writer.write("PURCHASE REQUIREMENTS (" + (purchaseList == null ? 0 : purchaseList.size()) + ")");
            writer.newLine();
            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();
            if (purchaseList != null && !purchaseList.isEmpty()) {
                writer.write(String.format("%-6s %-25s %-10s %-15s", "ID", "Component Name", "Qty to Buy", "Est. Cost"));
                writer.newLine();
                for (PurchaseItem p : purchaseList) {
                    writer.write(String.format("%-6d %-25s %-10d %-15s",
                            p.componentId(), truncate(p.componentName(), 25), p.quantityToPurchase(), p.estimatedCost()));
                    writer.newLine();
                }
            } else {
                writer.write("All component requirements are met by existing inventory.");
                writer.newLine();
            }
            writer.newLine();

            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();
            writer.write("EXPENSES (" + (expenses == null ? 0 : expenses.size()) + ")");
            writer.newLine();
            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();
            if (expenses != null && !expenses.isEmpty()) {
                writer.write(String.format("%-6s %-25s %-12s %-12s %-15s", "ID", "Description", "Amount", "Date", "Category"));
                writer.newLine();
                for (Expense exp : expenses) {
                    writer.write(String.format("%-6d %-25s %-12s %-12s %-15s",
                            exp.getExpenseId(), truncate(exp.getDescription(), 25), exp.getAmount(), exp.getExpenseDate(), exp.getCategory()));
                    writer.newLine();
                }
            } else {
                writer.write("No expenses logged.");
                writer.newLine();
            }
        }

        return filePath;
    }

    public static Path exportToCsv(Project project,
                                   List<BuildStep> steps,
                                   List<Expense> expenses) throws IOException {
        Path dir = Paths.get(REPORTS_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String filename = String.format("project_%d_%s.csv", project.getProjectId(),
                LocalDateTime.now().format(FILE_DATE_FMT));
        Path filePath = dir.resolve(filename);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write("SECTION,ID,NAME_OR_DESC,STATUS_OR_CAT,HOURS_OR_AMT,DEADLINE_OR_DATE");
            writer.newLine();

            writer.write(String.format("PROJECT,%d,\"%s\",%s,%s,%s",
                    project.getProjectId(),
                    escapeCsv(project.getName()),
                    project.getClass().getSimpleName(),
                    project.getBudget(),
                    project.getDeadline()));
            writer.newLine();

            if (steps != null) {
                for (BuildStep s : steps) {
                    writer.write(String.format("BUILD_STEP,%d,\"%s\",%s,%.2f,%s",
                            s.getStepId(),
                            escapeCsv(s.getName()),
                            s.getStatus(),
                            s.getEstimatedHours(),
                            s.getDeadline() == null ? "" : s.getDeadline().toString()));
                    writer.newLine();
                }
            }

            if (expenses != null) {
                for (Expense e : expenses) {
                    writer.write(String.format("EXPENSE,%d,\"%s\",%s,%s,%s",
                            e.getExpenseId(),
                            escapeCsv(e.getDescription()),
                            e.getCategory(),
                            e.getAmount(),
                            e.getExpenseDate()));
                    writer.newLine();
                }
            }
        }

        return filePath;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 3) + "...";
    }

    private static String escapeCsv(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
}

