package com.buildtrack.threads;

import com.buildtrack.model.BuildStep;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.Project;
import com.buildtrack.repository.BuildStepRepository;
import com.buildtrack.repository.ProjectRepository;
import com.buildtrack.service.BuildStepService;
import com.buildtrack.service.ProjectService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DeadlineMonitorTest {

    @Test
    void detectsOverdueStepsCorrectly() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        BuildStep overdueStep = new BuildStep(
                101, 1, "Wiring Harness", "Assemble wiring",
                8.0, 2.0, today.minusDays(4),
                BuildStep.Status.IN_PROGRESS, BuildStep.Priority.HIGH
        );

        List<DeadlineAlert> alerts = DeadlineMonitor.evaluateSteps(List.of(overdueStep), today, 3);
        assertEquals(1, alerts.size());

        DeadlineAlert alert = alerts.get(0);
        assertEquals(101, alert.stepId());
        assertEquals(DeadlineAlert.AlertType.OVERDUE, alert.alertType());
        assertEquals(4, alert.daysDifference());
        assertTrue(alert.message().contains("OVERDUE by 4 day(s)"));
    }

    @Test
    void detectsDueTodayStepsCorrectly() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        BuildStep dueTodayStep = new BuildStep(
                102, 1, "Sensor Calibration", "Calibrate IMU",
                4.0, 0.0, today,
                BuildStep.Status.PENDING, BuildStep.Priority.MEDIUM
        );

        List<DeadlineAlert> alerts = DeadlineMonitor.evaluateSteps(List.of(dueTodayStep), today, 3);
        assertEquals(1, alerts.size());

        DeadlineAlert alert = alerts.get(0);
        assertEquals(102, alert.stepId());
        assertEquals(DeadlineAlert.AlertType.DUE_TODAY, alert.alertType());
        assertEquals(0, alert.daysDifference());
        assertTrue(alert.message().contains("is DUE TODAY"));
    }

    @Test
    void detectsApproachingDeadlinesWithinConfiguredWarningDays() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        BuildStep approachingStep = new BuildStep(
                103, 1, "Chassis 3D Print", "Print mechanical frame",
                12.0, 0.0, today.plusDays(2),
                BuildStep.Status.PENDING, BuildStep.Priority.MEDIUM
        );

        List<DeadlineAlert> alerts = DeadlineMonitor.evaluateSteps(List.of(approachingStep), today, 3);
        assertEquals(1, alerts.size());

        DeadlineAlert alert = alerts.get(0);
        assertEquals(103, alert.stepId());
        assertEquals(DeadlineAlert.AlertType.APPROACHING, alert.alertType());
        assertEquals(2, alert.daysDifference());
        assertTrue(alert.message().contains("APPROACHING in 2 day(s)"));
    }

    @Test
    void ignoresDeadlinesBeyondWarningPeriod() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        BuildStep futureStep = new BuildStep(
                104, 1, "Final Testing", "System validation",
                10.0, 0.0, today.plusDays(10),
                BuildStep.Status.PENDING, BuildStep.Priority.LOW
        );

        List<DeadlineAlert> alerts = DeadlineMonitor.evaluateSteps(List.of(futureStep), today, 3);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void ignoresCompletedAndCancelledStepsRegardlessOfDate() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        BuildStep completedOverdue = new BuildStep(
                105, 1, "Procurement", "Buy parts",
                2.0, 2.0, today.minusDays(5),
                BuildStep.Status.COMPLETED, BuildStep.Priority.HIGH
        );
        BuildStep cancelledOverdue = new BuildStep(
                106, 1, "Alternate Driver", "Test alt driver",
                2.0, 0.0, today.minusDays(3),
                BuildStep.Status.CANCELLED, BuildStep.Priority.LOW
        );

        List<DeadlineAlert> alerts = DeadlineMonitor.evaluateSteps(
                List.of(completedOverdue, cancelledOverdue), today, 5
        );
        assertTrue(alerts.isEmpty(), "Completed or cancelled steps should never trigger deadline alerts.");
    }

    @Test
    void validatesConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () ->
                new DeadlineMonitor(new ProjectService(), new BuildStepService(), 0, 3, null));
        assertThrows(IllegalArgumentException.class, () ->
                new DeadlineMonitor(new ProjectService(), new BuildStepService(), 1000, -1, null));
        assertThrows(NullPointerException.class, () ->
                new DeadlineMonitor(null, new BuildStepService(), 1000, 3, null));
    }

    @Test
    void managesThreadLifecycleCorrectly() {
        ProjectService dummyProjectService = new ProjectService(new ProjectRepository() {
            @Override
            public List<Project> findAll() {
                return List.of();
            }
        });
        BuildStepService dummyStepService = new BuildStepService(new BuildStepRepository() {
            @Override
            public List<BuildStep> findByProjectId(int projectId) {
                return List.of();
            }
        }, new ProjectRepository(), null);

        DeadlineMonitor monitor = new DeadlineMonitor(
                dummyProjectService, dummyStepService, 50_000L, 3, null
        );

        assertFalse(monitor.isRunning(), "Monitor should not be running before start().");

        monitor.start();
        assertTrue(monitor.isRunning(), "Monitor should report running state after start().");

        // Verify duplicate start prevention
        assertThrows(IllegalStateException.class, monitor::start,
                "Starting an already running monitor should throw IllegalStateException.");

        monitor.stop();
        assertFalse(monitor.isRunning(), "Monitor should terminate and report not running after stop().");

        // Calling stop() again should be a safe no-op
        assertDoesNotThrow(monitor::stop);
    }

    @Test
    void dispatchesAlertsToConfiguredConsumer() throws Exception {
        LocalDate today = LocalDate.now();
        Project dummyProject = new HardwareProject(
                1, "Autonomous Rover", "Robotics test",
                today.minusDays(10), today.plusDays(20), BigDecimal.valueOf(500)
        );
        BuildStep dummyStep = new BuildStep(
                201, 1, "Motor Driver Assembly", "Solder motor driver",
                5.0, 0.0, today.plusDays(1),
                BuildStep.Status.PENDING, BuildStep.Priority.HIGH
        );

        ProjectService dummyProjectService = new ProjectService(new ProjectRepository() {
            @Override
            public List<Project> findAll() {
                return List.of(dummyProject);
            }
        });
        BuildStepService dummyStepService = new BuildStepService(new BuildStepRepository() {
            @Override
            public List<BuildStep> findByProjectId(int projectId) {
                return List.of(dummyStep);
            }
        }, new ProjectRepository(), null);

        List<DeadlineAlert> receivedAlerts = new ArrayList<>();
        DeadlineMonitor monitor = new DeadlineMonitor(
                dummyProjectService, dummyStepService, 60_000L, 3, receivedAlerts::add
        );

        List<DeadlineAlert> scanned = monitor.checkDeadlines();
        assertEquals(1, scanned.size());
        assertEquals(1, receivedAlerts.size());
        assertEquals(201, receivedAlerts.get(0).stepId());
        assertEquals(DeadlineAlert.AlertType.APPROACHING, receivedAlerts.get(0).alertType());
    }

    @Test
    void handlesExceptionsGracefullyWithoutCrashing() {
        AtomicBoolean exceptionHandled = new AtomicBoolean(false);
        ProjectService failingProjectService = new ProjectService(new ProjectRepository() {
            @Override
            public List<Project> findAll() throws SQLException {
                throw new SQLException("Simulated database connection loss");
            }
        });

        DeadlineMonitor monitor = new DeadlineMonitor(
                failingProjectService, new BuildStepService(), 500L, 3, null
        ) {
            @Override
            protected void handleException(Exception e) {
                exceptionHandled.set(true);
            }
        };

        // Start briefly then stop
        monitor.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        monitor.stop();

        assertTrue(exceptionHandled.get(), "Database exceptions should be caught and routed to handleException.");
        assertFalse(monitor.isRunning(), "Monitor should stop cleanly even after encountering an exception.");
    }
}

