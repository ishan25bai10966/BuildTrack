package com.buildtrack.threads;

import java.time.LocalDate;

public record DeadlineAlert(
        int stepId,
        int projectId,
        String stepName,
        LocalDate deadline,
        AlertType alertType,
        long daysDifference,
        String message
) {
    public enum AlertType {
        OVERDUE,
        DUE_TODAY,
        APPROACHING
    }
}

