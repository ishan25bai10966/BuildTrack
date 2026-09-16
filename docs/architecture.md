# BuildTrack System Architecture & Design Documentation

## 1. System Overview & Architectural Pattern
BuildTrack is structured around a classic **4-Tier Layered Architecture** emphasizing strict separation of concerns, high cohesion, and loose coupling.

```mermaid
flowchart TD
    subgraph UI ["Presentation Layer (com.buildtrack.ui)"]
        CLI["BuildTrackCli (Console UI)"]
        Exporter["ReportExporter (File I/O)"]
    end

    subgraph Service ["Service / Business Layer (com.buildtrack.service, com.buildtrack.threads)"]
        PS["ProjectService"]
        BSS["BuildStepService"]
        CS["ComponentService"]
        IS["InventoryService"]
        BS["BudgetService"]
        FS["FeasibilityService"]
        ES["EquipmentService"]
        DM["DeadlineMonitor (Thread)"]
    end

    subgraph Repo ["Data Access Layer (com.buildtrack.repository)"]
        PR["ProjectRepository"]
        BSR["BuildStepRepository"]
        CR["ComponentRepository"]
        PCR["ProjectComponentRepository"]
        IR["InventoryRepository"]
        ER["ExpenseRepository"]
        EqR["EquipmentRepository"]
        TDR["TaskDependencyRepository"]
    end

    subgraph DB ["Database Layer"]
        PG[("PostgreSQL 18 Database")]
        DMgr["DatabaseManager (JDBC)"]
    end

    CLI --> PS & BSS & CS & IS & BS & FS & ES
    CLI --> Exporter
    CLI --> DM
    DM -.-> BSS
    PS --> PR
    BSS --> BSR & TDR
    CS --> CR
    IS --> IR & PCR & CR
    BS --> ER & PCR & CR
    FS --> PS & BSS & BS & IS & TDR
    ES --> EqR
    PR & BSR & CR & PCR & IR & ER & EqR & TDR --> DMgr
    DMgr --> PG
```

---

## 2. Layer Responsibilities

### 1. Presentation Layer (`com.buildtrack.ui`)
- **`BuildTrackCli`**: Manages the terminal console loop, presents structured menus, validates user input (integers, big decimals, dates), catches business and SQL exceptions gracefully, and formats tabular data.
- **`ReportExporter`**: Handles file output operations, generating human-readable audit text reports (`.txt`) and comma-separated value spreadsheets (`.csv`) in the `reports/` folder.

### 2. Business Logic / Service Layer (`com.buildtrack.service` & `com.buildtrack.threads`)
- **`ProjectService`**: Validates project attributes (name, dates, budget non-negativity) and handles project lifecycle operations.
- **`BuildStepService`**: Manages step progress calculation, status transitions, prerequisite checks, and cycle detection.
- **`ComponentService`**: Validates component definitions and pricing before catalog entry.
- **`InventoryService`**: Computes component availability for projects, reconciles BOM requirements against stock, and generates itemized procurement purchase lists.
- **`BudgetService`**: Computes total required funds (BOM costs + recorded expenses), calculates remaining budget and utilization percentages, and assigns warning statuses.
- **`FeasibilityService`**: Runs multi-metric feasibility analysis (time, budget, components, dependencies, deadlines) and computes a composite Health Score (0–100) with actionable recommendations.
- **`EquipmentService`**: Coordinates lab machinery registration and operational availability state.
- **`DeadlineMonitor`**: Implements `Runnable` in an autonomous background thread to periodically scan build step deadlines and record `DeadlineAlert` entities.

### 3. Data Access Layer (`com.buildtrack.repository`)
- Provides clean CRUD interfaces for relational entities using pure JDBC `PreparedStatement` queries and mapping result sets into domain model objects.
- Handles SQL parameterization, preventing SQL injection vulnerabilities.

### 4. Database Layer (`com.buildtrack.database` & PostgreSQL)
- **`DatabaseManager`**: Centralizes JDBC connection lifecycle management, retrieving database credentials securely from environment variables.
- **PostgreSQL 18**: Relational persistence engine enforcing data integrity with primary keys, identity columns, foreign keys (`ON DELETE CASCADE` / `ON DELETE RESTRICT`), and check constraints.

---

## 3. Use Case Diagram

```mermaid
flowchart LR
    User((Engineering Student))

    subgraph BuildTrack ["BuildTrack System"]
        UC1["Create & View Projects"]
        UC2["Define Build Steps & Prerequisite Dependencies"]
        UC3["Manage BOM & Check Inventory Availability"]
        UC4["Record Actual Expenses & Monitor Budget"]
        UC5["Analyze Project Feasibility & Health Score"]
        UC6["Manage Lab Equipment Availability"]
        UC7["Export Audit Reports (TXT / CSV)"]
        UC8["Background Deadline Monitoring"]
    end

    User --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    User --> UC5
    User --> UC6
    User --> UC7
    UC8 -.-> User
```

---

## 4. Class & Component Diagram

```mermaid
classDiagram
    class Project {
        <<abstract>>
        -int projectId
        -String name
        -String description
        -LocalDate startDate
        -LocalDate deadline
        -BigDecimal budget
        -ProjectType projectType
        +calculateEstimatedCost()* BigDecimal
        +calculateEstimatedDuration()* int
    }

    class HardwareProject {
        +calculateEstimatedCost() BigDecimal
        +calculateEstimatedDuration() int
    }

    class IoTProject {
        +calculateEstimatedCost() BigDecimal
        +calculateEstimatedDuration() int
    }

    class MechanicalProject {
        +calculateEstimatedCost() BigDecimal
        +calculateEstimatedDuration() int
    }

    Project <|-- HardwareProject
    Project <|-- IoTProject
    Project <|-- MechanicalProject

    class BuildStep {
        -int stepId
        -int projectId
        -String name
        -String description
        -double estimatedHours
        -double actualHours
        -LocalDate deadline
        -Status status
        -Priority priority
    }

    class Component {
        -int componentId
        -String name
        -String category
        -String unit
        -BigDecimal unitCost
    }

    class ProjectComponent {
        -int projectId
        -int componentId
        -int quantityRequired
    }

    class TaskDependency {
        -int stepId
        -int prerequisiteStepId
    }

    class Expense {
        -int expenseId
        -int projectId
        -String description
        -BigDecimal amount
        -LocalDate expenseDate
        -String category
    }

    class Equipment {
        -int equipmentId
        -String name
        -String category
        -boolean available
    }

    Project "1" *-- "*" BuildStep
    Project "1" *-- "*" ProjectComponent
    Component "1" -- "*" ProjectComponent
    BuildStep "1" -- "*" TaskDependency : depends on
    Project "1" *-- "*" Expense
```

---

## 5. Sequence Diagram: Feasibility Analysis Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student as User
    participant CLI as BuildTrackCli
    participant FS as FeasibilityService
    participant PS as ProjectService
    participant BSS as BuildStepService
    participant BS as BudgetService
    participant IS as InventoryService

    Student->>CLI: Select 7. Feasibility Analysis (projectId)
    CLI->>FS: analyzeProject(projectId)
    FS->>PS: findProject(projectId)
    PS-->>FS: Project entity
    FS->>BSS: findBuildSteps(projectId)
    BSS-->>FS: List~BuildStep~
    FS->>BS: getBudgetSummary(project)
    BS-->>FS: BudgetSummary record
    FS->>IS: getProjectComponentAvailability(projectId)
    IS-->>FS: ComponentAvailability record
    FS->>BSS: validatePrerequisites(stepId)
    BSS-->>FS: Blocked step evaluation
    FS->>FS: Evaluate Time, Budget, Components, Dependencies, Deadlines
    FS->>FS: Compute Health Score (0-100) & Recommendations
    FS-->>CLI: FeasibilityResult record
    CLI->>Student: Display Formatted Health Report & Alerts
```

---

## 6. Background DeadlineMonitor Thread Flow

```mermaid
sequenceDiagram
    autonumber
    participant CLI as BuildTrackCli
    participant Thread as Thread (DeadlineMonitor)
    participant BSS as BuildStepService
    participant Alerts as CopyOnWriteArrayList~DeadlineAlert~

    CLI->>Thread: start() [isDaemon = true]
    loop While running and not interrupted
        Thread->>BSS: findBuildSteps(projectId) for all projects
        BSS-->>Thread: steps
        Thread->>Thread: Check if deadline is overdue or approaching (<= 3 days)
        alt Alert Triggered
            Thread->>Alerts: add(new DeadlineAlert(...))
            Thread->>CLI: Log alert notification to console
        end
        Thread->>Thread: Thread.sleep(pollIntervalMillis) [60000ms]
    end
    CLI->>Thread: stop() -> interrupt() & join()
```

---

## 7. Report Generation Flow
1. User requests report generation for a project from the CLI.
2. `BuildTrackCli` invokes `budgetService.getBudgetSummary()`, `feasibilityService.analyzeProject()`, `buildStepService.findBuildSteps()`, `inventoryService.generatePurchaseList()`, and `expenseRepository.findByProjectId()`.
3. `ReportExporter` creates the `reports/` directory if absent via `java.nio.file.Files.createDirectories`.
4. `ReportExporter.exportToTxt()` formats a complete plain-text audit document via `PrintWriter` and `BufferedWriter`.
5. `ReportExporter.exportToCsv()` formats a tabular CSV export using escaped strings and comma delimiters.
6. The CLI displays absolute file paths confirming successful disk persistence.

