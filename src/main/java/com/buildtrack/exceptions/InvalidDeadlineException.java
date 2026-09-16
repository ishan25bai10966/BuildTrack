package com.buildtrack.exceptions;

public class InvalidDeadlineException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public InvalidDeadlineException(String message) {
        super(message);
    }
}
