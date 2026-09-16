package com.buildtrack.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public class Expense {

    private int expenseId;
    private int projectId;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String category;

    public Expense() {
    }

    public Expense(int expenseId, int projectId, String description,
                   BigDecimal amount, LocalDate expenseDate, String category) {
        this.expenseId = expenseId;
        this.projectId = projectId;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.category = category;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
