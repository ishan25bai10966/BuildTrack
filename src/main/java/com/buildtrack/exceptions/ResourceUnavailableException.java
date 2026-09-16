package com.buildtrack.exceptions;

public class ResourceUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ResourceUnavailableException(String message) {
        super(message);
    }
}
