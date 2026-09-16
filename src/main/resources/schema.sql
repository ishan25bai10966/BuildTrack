-- BuildTrack PostgreSQL schema (apply manually to the buildtrack database).
-- This file is intentionally not run by the application.

CREATE TABLE project (
    project_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    start_date DATE,
    deadline DATE,
    budget NUMERIC(12, 2) NOT NULL CHECK (budget >= 0),
    project_type VARCHAR(20) NOT NULL
        CHECK (project_type IN ('HARDWARE', 'MECHANICAL', 'IOT')),
    CHECK (deadline IS NULL OR start_date IS NULL OR deadline >= start_date)
);

CREATE TABLE component (
    component_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    unit_cost NUMERIC(12, 2) NOT NULL CHECK (unit_cost >= 0)
);

CREATE TABLE equipment (
    equipment_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE build_step (
    step_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES project(project_id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    estimated_hours NUMERIC(10, 2) NOT NULL CHECK (estimated_hours >= 0),
    actual_hours NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (actual_hours >= 0),
    deadline DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED', 'CANCELLED')),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE project_component (
    project_id INTEGER NOT NULL REFERENCES project(project_id) ON DELETE CASCADE,
    component_id INTEGER NOT NULL REFERENCES component(component_id) ON DELETE RESTRICT,
    quantity_required INTEGER NOT NULL CHECK (quantity_required > 0),
    PRIMARY KEY (project_id, component_id)
);

CREATE TABLE inventory_item (
    inventory_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id INTEGER NOT NULL UNIQUE REFERENCES component(component_id) ON DELETE CASCADE,
    quantity_available INTEGER NOT NULL DEFAULT 0 CHECK (quantity_available >= 0)
);

CREATE TABLE expense (
    expense_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES project(project_id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    expense_date DATE NOT NULL,
    category VARCHAR(100) NOT NULL
);

CREATE TABLE task_dependency (
    step_id INTEGER NOT NULL REFERENCES build_step(step_id) ON DELETE CASCADE,
    prerequisite_step_id INTEGER NOT NULL REFERENCES build_step(step_id) ON DELETE CASCADE,
    PRIMARY KEY (step_id, prerequisite_step_id),
    CHECK (step_id <> prerequisite_step_id)
);

CREATE INDEX idx_build_step_project_id ON build_step(project_id);
CREATE INDEX idx_expense_project_id ON expense(project_id);
CREATE INDEX idx_task_dependency_prerequisite ON task_dependency(prerequisite_step_id);
