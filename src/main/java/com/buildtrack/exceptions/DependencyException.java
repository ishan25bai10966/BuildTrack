package com.buildtrack.exceptions;

public class DependencyException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DependencyException(String message) {
        super(message);
    }
}
