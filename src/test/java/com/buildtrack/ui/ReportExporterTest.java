package com.buildtrack.ui;

import com.buildtrack.model.BudgetSummary;
import com.buildtrack.model.BuildStep;
import com.buildtrack.model.Expense;
import com.buildtrack.model.FeasibilityResult;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.Project;
import com.buildtrack.model.PurchaseItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportExporterTest {

    @Test
    void exportsProjectReportToTxtAndCsv() throws IOException {
        Project project = new HardwareProject(
                999, "Robotic Arm Prototype", "6-DOF manipulator",
                LocalDate.now(), LocalDate.now().plusDays(30), new BigDecimal("12000.00")
        );

        BudgetSummary budget = new BudgetSummary(
                new BigDecimal("12000.00"), new BigDecimal("8000.00"),
                new BigDecimal("1500.00"), new BigDecimal("9500.00"),
                new BigDecimal("2500.00"), new BigDecimal("79.17"),
                BudgetSummary.BudgetStatus.WARNING
        );

        FeasibilityResult feasibility = new FeasibilityResult(
                project.getName(), 85,
                FeasibilityResult.FeasibilityStatus.ON_TRACK,
                FeasibilityResult.FeasibilityStatus.AT_RISK,
                FeasibilityResult.FeasibilityStatus.ON_TRACK,
                FeasibilityResult.FeasibilityStatus.ON_TRACK,
                FeasibilityResult.FeasibilityStatus.ON_TRACK,
                FeasibilityResult.FeasibilityStatus.ON_TRACK,
                40.0, 30L, new BigDecimal("2500.00"),
                10, 8, 2, 0, 0,
                List.of("Budget utilization reached warning threshold (79.17%).")
        );

        List<BuildStep> steps = List.of(
                new BuildStep(1, 999, "CAD Design", "Create 3D models", 15.0, 15.0,
                        LocalDate.now().plusDays(5), BuildStep.Status.COMPLETED, BuildStep.Priority.HIGH),
                new BuildStep(2, 999, "Servo Calibration", "Calibrate feedback loop", 10.0, 0.0,
                        LocalDate.now().plusDays(15), BuildStep.Status.PENDING, BuildStep.Priority.MEDIUM)
        );

        List<PurchaseItem> purchases = List.of(
                new PurchaseItem(10, "MG996R Servos", 2, new BigDecimal("900.00"))
        );

        List<Expense> expenses = List.of(
                new Expense(1, 999, "Aluminium brackets", new BigDecimal("1500.00"),
                        LocalDate.now(), "Hardware")
        );

        Path txtPath = ReportExporter.exportToTxt(project, budget, feasibility, steps, purchases, expenses);
        Path csvPath = ReportExporter.exportToCsv(project, steps, expenses);

        try {
            assertTrue(Files.exists(txtPath), "TXT report file should exist.");
            assertTrue(Files.size(txtPath) > 0, "TXT report should not be empty.");
            String txtContent = Files.readString(txtPath);
            assertTrue(txtContent.contains("Robotic Arm Prototype"));
            assertTrue(txtContent.contains("FEASIBILITY ASSESSMENT"));
            assertTrue(txtContent.contains("BUDGET SUMMARY"));

            assertTrue(Files.exists(csvPath), "CSV report file should exist.");
            assertTrue(Files.size(csvPath) > 0, "CSV report should not be empty.");
            String csvContent = Files.readString(csvPath);
            assertTrue(csvContent.contains("SECTION,ID,NAME_OR_DESC,STATUS_OR_CAT,HOURS_OR_AMT,DEADLINE_OR_DATE"));
            assertTrue(csvContent.contains("Robotic Arm Prototype"));
        } finally {
            Files.deleteIfExists(txtPath);
            Files.deleteIfExists(csvPath);
        }
    }
}

