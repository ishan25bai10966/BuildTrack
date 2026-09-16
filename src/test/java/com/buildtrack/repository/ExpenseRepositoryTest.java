package com.buildtrack.repository;

import com.buildtrack.model.Expense;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsUpdatesAndDeletesExpense() throws Exception {
        var project = createProject();
        ExpenseRepository repository = new ExpenseRepository();
        Expense expense = new Expense(0, project.getProjectId(), "Test expense",
                new BigDecimal("125.50"), LocalDate.now(), "Materials");
        int expenseId = repository.save(expense);
        try {
            assertEquals(expenseId, expense.getExpenseId());
            expense.setAmount(new BigDecimal("150.00"));
            repository.update(expense);
            assertEquals(new BigDecimal("150.00"), repository.findById(expenseId).getAmount());
        } finally {
            repository.delete(expenseId);
            deleteProject(project);
        }
        assertNull(repository.findById(expenseId));
    }
}
