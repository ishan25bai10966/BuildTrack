# BuildTrack — Academic Project Statement

**Course**: CSE2006 — Programming in Java  
**Domain**: Engineering Project Planning & Feasibility Analysis  
**Application Type**: Console / CLI Application  

---

## 1. Problem Statement
Undergraduate engineering students frequently struggle with project execution when building physical prototypes in robotics, IoT, embedded systems, and mechanical engineering. These builds routinely suffer from premature budget exhaustion, uncoordinated component acquisition, unplanned task dependencies, and missed milestone deadlines. Existing generic project management software is overly complex, detached from bill-of-materials (BOM) stock validation, and fails to evaluate whether a proposed hardware prototype is genuinely feasible before the student commits financial and laboratory resources.

---

## 2. Project Scope
BuildTrack provides a targeted, console-based decision support and planning system that allows engineering students to model their project lifecycle. The system manages hardware bills of materials, validates inventory availability, enforces prerequisite build order, assesses financial viability, tracks background deadlines, and outputs structured audit documentation.

---

## 3. Target Users
- **Undergraduate Engineering Students**: Designing semester projects, hardware competitions, or capstone engineering builds.
- **Laboratory Mentors and Faculty**: Reviewing student project proposals, resource demands, and financial feasibility before allocating lab space and funds.
- **Student Hardware Teams**: Coordinating dependencies between circuit design, component acquisition, PCB fabrication, and testing.

---

## 4. Objectives
1. Implement a complete Object-Oriented Java application demonstrating the core tenets of the CSE2006 syllabus (Encapsulation, Polymorphism, Inheritance, Exception Handling, Collections, Threads, File I/O, JDBC).
2. Model engineering builds with domain-specific cost and duration estimation heuristics (`HardwareProject`, `IoTProject`, `MechanicalProject`).
3. Maintain a centralized PostgreSQL relational database ensuring data integrity through foreign keys, cascaded updates, and check constraints.
4. Implement an algorithmic Feasibility Engine providing an objective Health Score (0–100) across time, budget, inventory, and dependency dimensions.
5. Deploy a non-blocking background thread (`DeadlineMonitor`) to continuously track approaching and overdue milestones.
6. Provide full persistence export of audit reports in text (`.txt`) and tabular (`.csv`) formats.

---

## 5. High-Level Features
- **Project Domain Modeling**: Support for custom hardware, IoT, and mechanical engineering attributes.
- **Bill of Materials & Inventory Control**: Association of required components with workshop stock and automated generation of purchase shortage lists.
- **Prerequisite Task Dependency Engine**: Directed Acyclic Graph (DAG) validation to detect cycles and enforce sequential prerequisite completion.
- **Financial Ledger & Alerts**: Expense tracking with threshold-based budget alert levels (Normal, Warning, Critical, Over Budget).
- **Lab Equipment Tracking**: Shared machine registry with operational availability tracking.
- **Autonomous Deadline Monitor**: Periodic multithreaded inspection identifying imminent milestones and overdue tasks.
- **File Exporting System**: TXT and CSV report generator for academic review.

---

## 6. Major Modules
- **Module 1 — Core Domain & Data Layer**: Database connection management, domain entity hierarchy, and JDBC repositories.
- **Module 2 — Planning & Dependency Engine**: Step milestone transitions, prerequisite validation, and cycle detection.
- **Module 3 — Inventory & Procurement Engine**: BOM reconciliation, stock shortage calculation, and purchase planning.
- **Module 4 — Financial & Feasibility Engine**: Budget tracking, expense management, multi-criteria feasibility scoring, and recommendations.
- **Module 5 — Multithreading & Alerting Engine**: Background daemon thread polling step deadlines and raising alerts.
- **Module 6 — Presentation & Reporting Layer**: Interactive terminal menu interface and structured report exporters.

---

## 7. Expected Outcome
The resulting system enables engineering students to establish clear project boundaries, identify missing components before assembly starts, avoid circular or blocked tasks, monitor budget burn rates, and receive early warnings for approaching deadlines—all within a lightweight, resilient command-line environment.

---

## 8. Technologies Used
- **Programming Language**: Java 21 (JDK 21 LTS)
- **Architecture**: 4-Tier Layered Architecture (Model - Repository - Service - UI)
- **Database**: PostgreSQL 18 with JDBC Driver (`postgresql:42.7.7`)
- **Persistence Specifications**: Jakarta Persistence API & Hibernate Core
- **Build System**: Apache Maven 3.9+
- **Testing**: JUnit 5 Jupiter (`junit-jupiter:5.12.2`)

---

## 9. Project Boundaries (What BuildTrack Does NOT Attempt to Do)
To maintain academic focus, architectural clarity, and strict adherence to project constraints:
1. **No Graphical User Interface (GUI)**: BuildTrack does NOT use JavaFX, Swing, AWT, or web frontends. It is strictly a terminal CLI application.
2. **No Heavy Web Frameworks**: The application does NOT use Spring Boot, Jakarta EE servers, Microservices, or REST servers.
3. **No Artificial Intelligence / Heuristic ML**: Feasibility is computed via deterministic, auditable engineering formulas rather than black-box AI/ML models.
4. **No Direct Hardware Control**: BuildTrack does not interface with microcontrollers (Arduino/Raspberry Pi) via serial ports; it manages the planning and inventory data for such projects.
5. **No Distributed Cloud Orchestration**: BuildTrack is designed as an on-premise, single-workstation system connecting to a local PostgreSQL instance.

