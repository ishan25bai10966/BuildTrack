package com.buildtrack.threads;

import com.buildtrack.model.BuildStep;
import com.buildtrack.model.Project;
import com.buildtrack.service.BuildStepService;
import com.buildtrack.service.ProjectService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Background thread monitor that periodically inspects BuildTrack build steps
 * to identify overdue milestones and approaching deadlines.
 */
public class DeadlineMonitor implements Runnable {

    public static final int DEFAULT_WARNING_DAYS = 3;
    public static final long DEFAULT_CHECK_INTERVAL_MILLIS = 60_000L;

    private final ProjectService projectService;
    private final BuildStepService buildStepService;
    private final long checkIntervalMillis;
    private final int warningDays;
    private final Consumer<DeadlineAlert> alertConsumer;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();
    private Thread workerThread;

    public DeadlineMonitor() {
        this(new ProjectService(), new BuildStepService(), DEFAULT_CHECK_INTERVAL_MILLIS,
                DEFAULT_WARNING_DAYS, defaultAlertConsumer());
    }

    public DeadlineMonitor(ProjectService projectService, BuildStepService buildStepService) {
        this(projectService, buildStepService, DEFAULT_CHECK_INTERVAL_MILLIS,
                DEFAULT_WARNING_DAYS, defaultAlertConsumer());
    }

    public DeadlineMonitor(ProjectService projectService,
                           BuildStepService buildStepService,
                           long checkIntervalMillis,
                           int warningDays,
                           Consumer<DeadlineAlert> alertConsumer) {
        if (checkIntervalMillis <= 0) {
            throw new IllegalArgumentException("Check interval must be greater than zero.");
        }
        if (warningDays < 0) {
            throw new IllegalArgumentException("Warning days cannot be negative.");
        }
        this.projectService = Objects.requireNonNull(projectService, "ProjectService is required.");
        this.buildStepService = Objects.requireNonNull(buildStepService, "BuildStepService is required.");
        this.checkIntervalMillis = checkIntervalMillis;
        this.warningDays = warningDays;
        this.alertConsumer = alertConsumer != null ? alertConsumer : defaultAlertConsumer();
    }

    /**
     * Starts the monitor in a dedicated daemon background thread.
     * Throws IllegalStateException if the monitor is already running.
     */
    public void start() {
        synchronized (lifecycleLock) {
            if (running.get()) {
                throw new IllegalStateException("DeadlineMonitor is already running.");
            }
            running.set(true);
            workerThread = new Thread(this, "DeadlineMonitor-Worker");
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    /**
     * Signals the background thread to stop, interrupts any sleeping state,
     * and waits briefly for clean termination.
     */
    public void stop() {
        Thread threadToJoin;
        synchronized (lifecycleLock) {
            if (!running.get() || workerThread == null) {
                return;
            }
            running.set(false);
            workerThread.interrupt();
            threadToJoin = workerThread;
        }

        try {
            threadToJoin.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Checks whether the monitor thread is currently active.
     */
    public boolean isRunning() {
        synchronized (lifecycleLock) {
            return running.get() && workerThread != null && workerThread.isAlive();
        }
    }

    public int getWarningDays() {
        return warningDays;
    }

    public long getCheckIntervalMillis() {
        return checkIntervalMillis;
    }

    @Override
    public void run() {
        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    checkDeadlines();
                } catch (Exception e) {
                    handleException(e);
                }

                Thread.sleep(checkIntervalMillis);
            }
        } catch (InterruptedException e) {
            // Restore interrupted status and allow thread to terminate cleanly
            Thread.currentThread().interrupt();
        } finally {
            synchronized (lifecycleLock) {
                running.set(false);
            }
        }
    }

    /**
     * Executes a single deadline check across all projects and sends alerts
     * to the configured consumer.
     */
    public List<DeadlineAlert> checkDeadlines() throws SQLException {
        List<DeadlineAlert> alerts = scanDeadlines(LocalDate.now());
        for (DeadlineAlert alert : alerts) {
            alertConsumer.accept(alert);
        }
        return alerts;
    }

    /**
     * Queries all projects and their steps from services and evaluates them against a reference date.
     */
    public List<DeadlineAlert> scanDeadlines(LocalDate today) throws SQLException {
        List<DeadlineAlert> allAlerts = new ArrayList<>();
        List<Project> projects = projectService.findAllProjects();
        if (projects == null || projects.isEmpty()) {
            return allAlerts;
        }

        for (Project project : projects) {
            List<BuildStep> steps = buildStepService.findBuildSteps(project.getProjectId());
            if (steps != null && !steps.isEmpty()) {
                allAlerts.addAll(evaluateSteps(steps, today, warningDays));
            }
        }

        return allAlerts;
    }

    /**
     * Pure business logic evaluation of a list of build steps against a date and warning window.
     */
    public static List<DeadlineAlert> evaluateSteps(List<BuildStep> steps, LocalDate today, int warningDays) {
        List<DeadlineAlert> alerts = new ArrayList<>();
        if (steps == null || steps.isEmpty() || today == null) {
            return alerts;
        }

        for (BuildStep step : steps) {
            if (step.getDeadline() == null) {
                continue;
            }
            if (step.getStatus() == BuildStep.Status.COMPLETED
                    || step.getStatus() == BuildStep.Status.CANCELLED) {
                continue;
            }

            LocalDate deadline = step.getDeadline();

            if (deadline.isBefore(today)) {
                long overdueDays = ChronoUnit.DAYS.between(deadline, today);
                String msg = String.format("Build step '%s' (Project ID %d) is OVERDUE by %d day(s) (Deadline: %s).",
                        step.getName(), step.getProjectId(), overdueDays, deadline);
                alerts.add(new DeadlineAlert(step.getStepId(), step.getProjectId(), step.getName(),
                        deadline, DeadlineAlert.AlertType.OVERDUE, overdueDays, msg));
            } else if (deadline.isEqual(today)) {
                String msg = String.format("Build step '%s' (Project ID %d) is DUE TODAY (Deadline: %s).",
                        step.getName(), step.getProjectId(), deadline);
                alerts.add(new DeadlineAlert(step.getStepId(), step.getProjectId(), step.getName(),
                        deadline, DeadlineAlert.AlertType.DUE_TODAY, 0, msg));
            } else if (!deadline.isAfter(today.plusDays(warningDays))) {
                long remainingDays = ChronoUnit.DAYS.between(today, deadline);
                String msg = String.format("Build step '%s' (Project ID %d) deadline is APPROACHING in %d day(s) (Deadline: %s).",
                        step.getName(), step.getProjectId(), remainingDays, deadline);
                alerts.add(new DeadlineAlert(step.getStepId(), step.getProjectId(), step.getName(),
                        deadline, DeadlineAlert.AlertType.APPROACHING, remainingDays, msg));
            }
        }

        return alerts;
    }

    protected void handleException(Exception e) {
        System.err.println("[DeadlineMonitor] Non-fatal error checking build step deadlines: " + e.getMessage());
    }

    private static Consumer<DeadlineAlert> defaultAlertConsumer() {
        return alert -> System.out.println("[DEADLINE MONITOR ALERT] " + alert.message());
    }
}

