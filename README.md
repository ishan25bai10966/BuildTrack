# BuildTrack — Engineering Project Planning & Feasibility System

> **Course**: CSE2006 — Programming in Java  
> **Interface**: Console / CLI Application  
> **Build Tool**: Apache Maven (Java 21)  
> **Database**: PostgreSQL 18  
> **Verified Test Suite**: 31 / 31 Tests Passing (0 Failures, 0 Errors, 0 Skipped)

---

## 1. Overview
**BuildTrack** is a console-based engineering project planning, budgeting, dependency tracking, inventory validation, and feasibility evaluation system designed for engineering students. It empowers students building **Hardware**, **IoT**, **Robotics**, **Mechanical**, and **Embedded Systems** prototypes to plan their builds systematically, validate prerequisites, evaluate budget and time constraints before ordering parts, and monitor task deadlines concurrently.

---

## 2. Problem Statement
Engineering students undertaking capstone and semester laboratory projects routinely face:
- **Budget Overruns**: Purchasing expensive components without accounting for ancillary costs (shipping, fasteners, tooling).
- **Inventory Mismatches**: Discovering midway through an assembly that critical sensors, ICs, or brackets are out of stock.
- **Dependency Bottlenecks**: Attempting to solder or test subsystems before firmware or PCB etching is completed, causing avoidable delays.
- **Missed Deadlines**: Lacking proactive deadline tracking for sequential build milestones across multi-week development cycles.

BuildTrack resolves these challenges through a centralized command-line interface backed by a relational PostgreSQL database, a deterministic feasibility analysis engine, and an autonomous background deadline monitor.

---

## 3. Target Users
- **Undergraduate Engineering Students**: Working on capstone, mini-project, IoT, robotics, or embedded hardware builds.
- **Student Project Teams**: Collaborating on multidisciplinary hardware/software prototypes.
- **Academic Lab Instructors**: Reviewing student project feasibility, component requirements, and budget health before sanctioning lab resources.

---

## 4. Objectives
1. Provide an interactive, resilient console interface for end-to-end engineering project management.
2. Model polymorphic engineering domains (`HardwareProject`, `IoTProject`, `MechanicalProject`) with custom cost and duration estimation heuristics.
3. Manage bill-of-materials (BOM) and match requirements against existing workshop inventory to generate purchase lists.
4. Enforce acyclic, prerequisite-validated task dependency graphs for build steps.
5. Provide a deterministic **Feasibility Analysis Engine** calculating a multi-factor Health Score (0–100) across time, budget, components, and task dependencies.
6. Provide a non-blocking background **Deadline Monitor** thread that detects approaching and overdue milestones without stalling user workflows.
7. Export comprehensive audit reports in clean TXT and CSV formats.

---

## 5. Major Features
- **Interactive Console CLI**: Menu-driven navigation with input validation and zero GUI dependencies.
- **Polymorphic Project Types**: Domain-specific calculations for IoT cloud overhead, mechanical tolerance buffer, and hardware safety margins.
- **Component & Inventory Tracking**: BOM mapping, stock availability checks, and automatic purchase deficit calculation.
- **Task Dependency Management**: Directed prerequisite enforcement preventing out-of-order execution and detecting circular dependencies.
- **Budget & Expense Tracking**: Dynamic utilization metrics, deficit detection, and tiered budget warnings (Normal, Warning >75%, Critical >90%, Over Budget).
- **Lab Equipment Management**: Registration, category tagging, availability toggling, and lab resource readiness checks.
- **Multithreaded Deadline Monitor**: Background daemon thread polling project steps at configurable intervals and raising alerts.
- **Automated Report Exporter**: Production of formatted audit reports (`.txt`) and data exchange spreadsheets (`.csv`) in the `reports/` directory.

---

## 6. Functional Modules
1. **Project Management Module**: CRUD operations for projects, polymorphic duration/cost calculations, and search filters.
2. **Build Step & Dependency Module**: Step lifecycle transitions (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `BLOCKED`, `CANCELLED`), priority levels, and prerequisite validation.
3. **Component & Inventory Module**: Master component registry, project-component associations, and stock quantity tracking.
4. **Expense & Budget Module**: Actual expense ledger, running totals, and budget utilization analysis.
5. **Equipment Module**: Tracking shared laboratory machinery (soldering stations, 3D printers, oscilloscopes) and real-time availability.
6. **Feasibility Engine Module**: Algorithmic assessment of project health combining schedule slippage, component availability, budget margin, and dependency blocks into a composite score.
7. **Deadline Monitor Module**: Multithreaded background alerting service for impending or breached deadlines.
8. **Reporting Module**: File I/O subsystem exporting persistent text and CSV summaries.

---

## 7. Non-Functional Requirements
- **Reliability & Data Integrity**: Foreign key constraints, cascade rules, and check constraints enforced at the PostgreSQL database level.
- **Thread Safety**: Safe background monitor execution utilizing `volatile` flags, synchronized access, defensive copies, and proper thread interruption semantics.
- **Portability**: Standard Java 21 LTS with pure JDBC drivers executable across Linux, macOS, and Windows.
- **Maintainability & Clean Architecture**: Strict separation of concerns (Model-Repository-Service-UI) with zero circular package dependencies.
- **Security**: Database credentials supplied exclusively via environment variables; never hardcoded or printed.

---

## 8. Technology Stack
- **Language**: Java 21 (JDK 21)
- **Build & Dependency Tool**: Apache Maven 3.9+
- **Database Engine**: PostgreSQL 18 (Relational database)
- **Database Connectivity**: PostgreSQL JDBC Driver (`org.postgresql:postgresql:42.7.7`)
- **Persistence Foundation**: Jakarta Persistence API (`jakarta.persistence-api:3.2.0`) & Hibernate Core (`org.hibernate.orm:hibernate-core:6.6.36.Final`)
- **Testing Framework**: JUnit 5 Jupiter (`org.junit.jupiter:junit-jupiter:5.12.2`)
- **CLI / Console**: Native standard I/O (`System.in`, `System.out`, `java.util.Scanner`)

---

## 9. Java Concepts Demonstrated (CSE2006 Syllabus Mapping)
- **Object-Oriented Programming**: Encapsulation, robust domain models with private state and validated mutators.
- **Inheritance & Polymorphism**: Abstract base class `Project` extended by `HardwareProject`, `IoTProject`, and `MechanicalProject` with dynamic method dispatch for `calculateEstimatedCost()` and `calculateEstimatedDuration()`.
- **Abstract Classes & Interfaces**: `Project` abstract class; `Runnable` interface implementation in `DeadlineMonitor`.
- **Enums**: Strong type safety with `BuildStep.Status`, `BuildStep.Priority`, `Project.ProjectType`, `BudgetSummary.BudgetStatus`, and `FeasibilityResult.FeasibilityStatus`.
- **Collections Framework & Streams**: Heavy usage of `List`, `Map`, `Set`, and Stream pipelines (`filter`, `map`, `collect`, `sorted`).
- **Custom Exception Handling**: Hierarchical domain exceptions under `com.buildtrack.exceptions` (`InvalidProjectException`, `BudgetExceededException`, `DependencyException`, `InvalidDeadlineException`, `ResourceUnavailableException`).
- **Multithreading**: Thread lifecycle management (`start()`, `interrupt()`, `join()`), worker daemon threads, `volatile boolean running`, and thread-safe shared state.
- **Java I/O**: `java.nio.file.Path`, `Files.createDirectories`, `PrintWriter`, and `BufferedWriter` for report exports.
- **JDBC Database Access**: `DriverManager`, parameterized `PreparedStatement`, `ResultSet`, transactional generation keys, and SQL error wrapping.

---

## 10. Architecture & Layer Responsibilities
BuildTrack follows an enterprise-grade 4-tier layered architecture:
```
+-------------------------------------------------------------+
|                      User / Console                         |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                     Presentation Layer                      |
|           BuildTrackCli  |  ReportExporter                  |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                   Business / Service Layer                  |
| ProjectService | BuildStepService | InventoryService        |
| BudgetService  | FeasibilityService | EquipmentService      |
| DeadlineMonitor (Background Thread)                         |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                     Data Access Layer                       |
| ProjectRepository | BuildStepRepository | ComponentRepository|
| ExpenseRepository | InventoryRepository | EquipmentRepository|
| TaskDependencyRepository | ProjectComponentRepository       |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|               Relational Database (PostgreSQL)              |
| 8 Tables: project, build_step, component, project_component,|
|   inventory_item, equipment, expense, task_dependency       |
+-------------------------------------------------------------+
```

---

## 11. Package Structure
```
com.buildtrack
├── Main.java                          # CLI Application Entry Point
├── database
│   └── DatabaseManager.java           # Centralized JDBC connection provider
├── exceptions
│   ├── BudgetExceededException.java
│   ├── DependencyException.java
│   ├── InvalidDeadlineException.java
│   ├── InvalidProjectException.java
│   └── ResourceUnavailableException.java
├── model
│   ├── BudgetSummary.java             # Record: Budget computation summary
│   ├── BuildStep.java                 # Entity: Milestone build step
│   ├── Component.java                 # Entity: Electronic/mechanical component
│   ├── ComponentAvailability.java     # Record: Missing inventory analysis
│   ├── Equipment.java                 # Entity: Workshop/lab equipment
│   ├── Expense.java                   # Entity: Financial transaction record
│   ├── FeasibilityResult.java         # Record: Multi-metric health analysis
│   ├── HardwareProject.java           # Domain subclass: General hardware builds
│   ├── InventoryItem.java             # Entity: Stock count mapping
│   ├── IoTProject.java                # Domain subclass: Connected IoT builds
│   ├── MechanicalProject.java         # Domain subclass: Mechanical prototypes
│   ├── Project.java                   # Abstract base entity
│   ├── ProjectComponent.java          # Associative entity: Project BOM
│   ├── PurchaseItem.java              # Record: Suggested procurement item
│   └── TaskDependency.java            # Associative entity: Prerequisite pairs
├── repository
│   ├── BuildStepRepository.java
│   ├── ComponentRepository.java
│   ├── EquipmentRepository.java
│   ├── ExpenseRepository.java
│   ├── InventoryRepository.java
│   ├── ProjectComponentRepository.java
│   ├── ProjectRepository.java
│   └── TaskDependencyRepository.java
├── service
│   ├── BudgetService.java
│   ├── BuildStepService.java
│   ├── ComponentService.java
│   ├── EquipmentService.java
│   ├── FeasibilityService.java
│   ├── InventoryService.java
│   └── ProjectService.java
├── threads
│   ├── DeadlineAlert.java             # Alert entity for background monitor
│   └── DeadlineMonitor.java           # Runnable background monitoring daemon
└── ui
    ├── BuildTrackCli.java             # Interactive menu console interface
    └── ReportExporter.java            # File I/O persistence engine (TXT/CSV)
```

---

## 12. Database & Schema Overview
BuildTrack uses 8 relational tables defined in [`src/main/resources/schema.sql`](src/main/resources/schema.sql):
1. `project`: Core entity storing project metadata, polymorphic `project_type`, budget, and dates.
2. `component`: Master component catalog with unit costs and category tagging.
3. `project_component`: Bill-of-materials linking projects to components with required quantities.
4. `inventory_item`: Current workshop warehouse stock for each component.
5. `build_step`: Step milestones with priority, estimated/actual hours, deadline, and completion status.
6. `task_dependency`: Directed DAG edges linking `step_id` to `prerequisite_step_id` (with self-dependency check).
7. `expense`: Actual expenditure ledger tracking payments against projects.
8. `equipment`: Shared lab apparatus registry with boolean availability status.

---

## 13. Application Workflow
```
 1. Main Launches BuildTrackCli
 2. Database connection verified via DatabaseManager
 3. DeadlineMonitor background thread spawned (polls steps every 60s)
 4. User navigates interactive CLI Main Menu:
    ├── 1. Dashboard            --> High-level health, overdue counts, equipment ratios
    ├── 2. Projects             --> Create (Hardware/IoT/Mechanical), list, view, update, delete
    ├── 3. Build Steps          --> Add steps, manage dependencies, log hours, update status
    ├── 4. Components           --> Master catalog CRUD
    ├── 5. Inventory            --> Stock management & purchase deficit list generation
    ├── 6. Expenses             --> Log actual expenses & inspect budget utilization
    ├── 7. Feasibility Analysis --> Algorithmic feasibility check & 0-100 Health Score
    ├── 8. Equipment            --> Lab machine registry & availability toggle
    ├── 9. Reports              --> Export audit files (TXT and CSV) to reports/
    └── 10. Exit                --> Clean shutdown & DeadlineMonitor thread termination
```

---

## 14. Installation & Setup

### Prerequisites
- **Java Development Kit**: JDK 21 or higher installed and configured.
- **Apache Maven**: Maven Wrapper is included in the repository, so a separate global Maven installation is not required.
- **PostgreSQL Database Server**: PostgreSQL 18 (or a compatible PostgreSQL version) running locally on port 5432.

### Step 1: Clone or Open Project
Navigate to the project root:
```bash
cd C:\Users\ishan\IdeaProjects\BuildTrack
```

### Step 2: Initialize Database Schema
In PostgreSQL (via `psql` or pgAdmin), create the database and apply the schema:
```sql
CREATE DATABASE buildtrack;
\c buildtrack
\i src/main/resources/schema.sql
```

---

## 15. Secure PostgreSQL Configuration
BuildTrack strictly avoids hardcoded database credentials in source code. Set your database credentials using environment variables:

**Windows (PowerShell)**:
```powershell
$env:BUILDTRACK_DB_PASSWORD="your_postgres_password"
# Optional overrides (defaults to localhost:5432/buildtrack and user postgres):
# $env:BUILDTRACK_DB_URL="jdbc:postgresql://localhost:5432/buildtrack"
# $env:BUILDTRACK_DB_USER="postgres"
```

**Linux / macOS (Bash)**:
```bash
export BUILDTRACK_DB_PASSWORD="your_postgres_password"
```

---

## 16. How to Run the CLI Application
Build and run the application using the included Maven Wrapper.

**Windows (PowerShell):**
```powershell
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.buildtrack.Main"
```

**Linux / macOS:**
```bash
./mvnw compile exec:java -Dexec.mainClass="com.buildtrack.Main"
```

Or package and run the JAR directly.

**Windows (PowerShell):**
```powershell
.\mvnw.cmd clean package
java -jar target/buildtrack-1.0-SNAPSHOT.jar
```

**Linux / macOS:**
```bash
./mvnw clean package
java -jar target/buildtrack-1.0-SNAPSHOT.jar
```

---

## 17. How to Run the Test Suite
Execute the comprehensive automated test suite using the included Maven Wrapper.

**Windows (PowerShell):**
```powershell
.\mvnw.cmd test
```

**Linux / macOS:**
```bash
./mvnw test
```

To run clean tests and produce reports.

**Windows (PowerShell):**
```powershell
.\mvnw.cmd clean test
```

**Linux / macOS:**
```bash
./mvnw clean test
```

---

## 18. Report Generation
BuildTrack features an automated file export engine:
- Selecting **Option 9 (Reports)** in the CLI generates:
  - **Audit Text Report**: `reports/project_<ID>_report.txt`
  - **Spreadsheet CSV Report**: `reports/project_<ID>_report.csv`
- Reports capture project metadata, budget summaries, feasibility scores, build steps with completion percentages, missing component purchase lists, and expense records.

---

## 19. Current Testing Status
All automated tests execute against live repositories, business services, multithreading components, and file exporters with 100% success rate:
- **Total Tests Run**: 31
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Result**: **BUILD SUCCESS**

| Test Class | Tests Run | Result |
| :--- | :---: | :---: |
| `com.buildtrack.database.DatabaseConnectionTest` | 2 | PASS |
| `com.buildtrack.repository.ProjectRepositoryTest` | 1 | PASS |
| `com.buildtrack.repository.BuildStepRepositoryTest` | 1 | PASS |
| `com.buildtrack.repository.ComponentRepositoryTest` | 1 | PASS |
| `com.buildtrack.repository.ProjectComponentRepositoryTest` | 1 | PASS |
| `com.buildtrack.repository.InventoryRepositoryTest` | 1 | PASS |
| `com.buildtrack.repository.EquipmentRepositoryTest` | 1 | PASS |
| `com.buildtrack.repository.ExpenseRepositoryTest` | 1 | PASS |
| `com.buildtrack.repository.TaskDependencyRepositoryTest` | 1 | PASS |
| `com.buildtrack.service.BusinessLogicTest` | 5 | PASS |
| `com.buildtrack.service.EquipmentServiceTest` | 3 | PASS |
| `com.buildtrack.service.FeasibilityServiceTest` | 3 | PASS |
| `com.buildtrack.threads.DeadlineMonitorTest` | 9 | PASS |
| `com.buildtrack.ui.ReportExporterTest` | 1 | PASS |
| **Total** | **31** | **ALL PASSED** |

---

## 20. Project Limitations
- **CLI-Only Interface**: No graphical windowing system (JavaFX or Swing) is provided, strictly fulfilling console requirements.
- **Single-User Console**: Concurrency is utilized internally for background monitoring, but the CLI itself assumes single-user interactive session semantics.
- **Local Database Scope**: Designed for local workstation PostgreSQL instances rather than distributed cloud clusters.

---

## 21. Future Enhancements
- **Gantt Chart CLI ASCII Visualizer**: Rendering terminal-based Gantt charts representing critical path analysis.
- **Supplier API Integration**: Live component price and inventory lookup against electronic component distributors.
- **Email/Webhook Notification Hooks**: Emitting automated webhooks when `DeadlineMonitor` detects overdue milestones.

---

## 22. Submission Metadata
- **Project**: BuildTrack
- **Syllabus**: CSE2006 Programming in Java
- **Language**: Core Java (JDK 21)


