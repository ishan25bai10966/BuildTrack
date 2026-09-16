# CSE2006 (Programming in Java) Concept Mapping

This document maps the implementation of **BuildTrack** directly to the core curriculum concepts of the **CSE2006 Programming in Java** syllabus. Every concept cited is directly referenced from actual, existing code files.

---

## 1. Classes and Objects
- **Implementation**: Realized across all entities under `com.buildtrack.model` and services under `com.buildtrack.service`.
- **Code Reference**:
  - `BuildStep` in [`src/main/java/com/buildtrack/model/BuildStep.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/BuildStep.java) represents milestone tasks with state variables (`stepId`, `projectId`, `name`, `estimatedHours`, `actualHours`, `status`, `priority`).
  - `Component` in [`src/main/java/com/buildtrack/model/Component.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/Component.java).

---

## 2. Constructors (Default, Parameterized, Overloaded)
- **Implementation**: Entities and services provide explicit default and overloaded parameterized constructors.
- **Code Reference**:
  - `Equipment` in [`src/main/java/com/buildtrack/model/Equipment.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/Equipment.java):
    ```java
    public Equipment() {}
    public Equipment(int equipmentId, String name, String category, boolean available) { ... }
    ```
  - `EquipmentService` in [`src/main/java/com/buildtrack/service/EquipmentService.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/service/EquipmentService.java) provides dependency-injection constructor overloading:
    ```java
    public EquipmentService() { this(new EquipmentRepository()); }
    public EquipmentService(EquipmentRepository equipmentRepository) { ... }
    ```

---

## 3. Encapsulation
- **Implementation**: All domain entity member fields are declared with `private` visibility and exposed only through typed public accessors and mutators with invariant preservation.
- **Code Reference**:
  - `Project` in [`src/main/java/com/buildtrack/model/Project.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/Project.java):
    ```java
    private BigDecimal budget;
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    ```

---

## 4. Inheritance
- **Implementation**: Specialized domain projects extend the common abstract base class `Project`.
- **Code Reference**:
  - Base class: `public abstract class Project` in [`src/main/java/com/buildtrack/model/Project.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/Project.java).
  - Derived classes:
    - `public class HardwareProject extends Project` in [`src/main/java/com/buildtrack/model/HardwareProject.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/HardwareProject.java).
    - `public class IoTProject extends Project` in [`src/main/java/com/buildtrack/model/IoTProject.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/IoTProject.java).
    - `public class MechanicalProject extends Project` in [`src/main/java/com/buildtrack/model/MechanicalProject.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/MechanicalProject.java).

---

## 5. Method Overriding & Abstract Classes
- **Implementation**: `Project` defines abstract methods which are overridden by each concrete subclass with domain-specific algorithms.
- **Code Reference**:
  - `Project.java`:
    ```java
    public abstract BigDecimal calculateEstimatedCost();
    public abstract int calculateEstimatedDuration();
    ```
  - `IoTProject.java`:
    ```java
    @Override
    public BigDecimal calculateEstimatedCost() {
        BigDecimal base = getBudget() != null ? getBudget() : BigDecimal.ZERO;
        return base.add(base.multiply(BigDecimal.valueOf(0.15))); // 15% cloud & comms overhead
    }
    ```
  - `MechanicalProject.java`: Overrides `calculateEstimatedDuration()` adding 20% safety margin for fabrication tolerances.

---

## 6. Polymorphism & Dynamic Method Dispatch
- **Implementation**: Collections of `Project` references invoke overridden methods at runtime, resolving to the appropriate subclass implementation dynamically.
- **Code Reference**:
  - In `BuildTrackCli.java` and `ReportExporter.java`:
    ```java
    Project project = projectService.findProject(id);
    BigDecimal cost = project.calculateEstimatedCost(); // Polymorphic dispatch
    int duration = project.calculateEstimatedDuration(); // Polymorphic dispatch
    ```

---

## 7. Interfaces
- **Implementation**: Standard Java `Runnable` interface implemented by `DeadlineMonitor`.
- **Code Reference**:
  - `public class DeadlineMonitor implements Runnable` in [`src/main/java/com/buildtrack/threads/DeadlineMonitor.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/threads/DeadlineMonitor.java).

---

## 8. Enumerations (Enums)
- **Implementation**: Strongly-typed enums prevent invalid domain states.
- **Code Reference**:
  - `BuildStep.Status`: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `BLOCKED`, `CANCELLED` in [`BuildStep.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/BuildStep.java).
  - `BuildStep.Priority`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` in [`BuildStep.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/BuildStep.java).
  - `Project.ProjectType`: `HARDWARE`, `MECHANICAL`, `IOT` in [`Project.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/Project.java).
  - `BudgetSummary.BudgetStatus`: `NORMAL`, `WARNING`, `CRITICAL`, `OVER_BUDGET` in [`BudgetSummary.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/BudgetSummary.java).
  - `FeasibilityResult.FeasibilityStatus`: `ON_TRACK`, `AT_RISK`, `CRITICAL` in [`FeasibilityResult.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/FeasibilityResult.java).

---

## 9. Java Records
- **Implementation**: Immutable data carrier records introduced in modern Java for composite results.
- **Code Reference**:
  - `BudgetSummary` in [`src/main/java/com/buildtrack/model/BudgetSummary.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/BudgetSummary.java).
  - `FeasibilityResult` in [`src/main/java/com/buildtrack/model/FeasibilityResult.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/FeasibilityResult.java).
  - `PurchaseItem` in [`src/main/java/com/buildtrack/model/PurchaseItem.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/PurchaseItem.java).
  - `ComponentAvailability` in [`src/main/java/com/buildtrack/model/ComponentAvailability.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/model/ComponentAvailability.java).

---

## 10. Collections Framework & Stream API
- **Implementation**: Comprehensive usage of `List`, `Map`, `Set`, `ArrayList`, `HashMap`, `HashSet`, `CopyOnWriteArrayList`, and Java Streams.
- **Code Reference**:
  - Cycle detection via DFS using `Set<Integer>` and `Map<Integer, List<Integer>>` in [`BuildStepService.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/service/BuildStepService.java).
  - Functional Stream filtering, mapping, and grouping:
    ```java
    public List<Equipment> findAvailableEquipment() throws SQLException {
        return equipmentRepository.findAll().stream()
                .filter(Equipment::isAvailable)
                .toList();
    }
    ```

---

## 11. Exception Handling & Custom Exceptions
- **Implementation**: Robust exception hierarchy distinguishing validation errors, budget breaches, and relational constraints.
- **Code Reference**:
  - Custom exceptions under `com.buildtrack.exceptions`:
    - `BudgetExceededException`: Incurring expenses exceeding budget caps.
    - `DependencyException`: Circular or invalid prerequisite relationships.
    - `InvalidDeadlineException`: Deadlines set chronologically prior to start dates.
    - `InvalidProjectException`: Blank project names or negative budget values.
    - `ResourceUnavailableException`: Missing equipment or out-of-stock items.
  - Granular `try-catch-finally` handling with informative error feedback throughout `BuildTrackCli`.

---

## 12. Multithreading & Concurrency
- **Implementation**: Asynchronous background monitoring via dedicated worker thread.
- **Code Reference**:
  - `DeadlineMonitor` in [`src/main/java/com/buildtrack/threads/DeadlineMonitor.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/threads/DeadlineMonitor.java).
  - **Thread Lifecycle**:
    - Creation: `workerThread = new Thread(this, "DeadlineMonitorWorker")`
    - Daemon configuration: `workerThread.setDaemon(true)`
    - Execution start: `workerThread.start()`
    - Interruption & Termination: `workerThread.interrupt()`, `workerThread.join()`
  - **Thread Safety**:
    - `volatile boolean running` ensuring cross-thread visibility of termination signals.
    - Synchronized `start()` and `stop()` preventing race conditions or duplicate worker threads.
    - Thread-safe collections (`CopyOnWriteArrayList<DeadlineAlert>`) and defensive copying in `getAlerts()`.

---

## 13. Java File I/O
- **Implementation**: Modern NIO.2 and classic stream writers exporting persistent file reports.
- **Code Reference**:
  - `ReportExporter` in [`src/main/java/com/buildtrack/ui/ReportExporter.java`](file:///C:/Users/ishan/IdeaProjects/BuildTrack/src/main/java/com/buildtrack/ui/ReportExporter.java):
    - Directory creation: `Files.createDirectories(reportDir)`
    - Character streaming: `new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))`
    - Formatted text printing (`printf`, `println`) and escaped CSV value generation.

---

## 14. JDBC (Java Database Connectivity)
- **Implementation**: Pure JDBC 4.2 data access across 8 repository implementations.
- **Code Reference**:
  - `DatabaseManager.getConnection()` using `DriverManager.getConnection()`.
  - Secure parameterized queries via `PreparedStatement` to prevent SQL injection.
  - Identity key retrieval via `Statement.RETURN_GENERATED_KEYS`.
  - Proper resource management using `try-with-resources` ensuring all `Connection`, `PreparedStatement`, and `ResultSet` objects close deterministically.

---

## 15. JPA / Jakarta Persistence Foundation
- **Implementation**: Project dependencies configured in `pom.xml` (`jakarta.persistence-api:3.2.0` and `hibernate-core:6.6.36.Final`) establishing full compatibility for Object-Relational Mapping (ORM) annotations on entities while preserving fast JDBC execution.

