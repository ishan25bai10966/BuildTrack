package com.buildtrack.exceptions;

public class BudgetExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public BudgetExceededException(String message) {
        super(message);
    }
}
