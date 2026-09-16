package com.buildtrack.exceptions;

public class InvalidProjectException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public InvalidProjectException(String message) {
        super(message);
    }
}
