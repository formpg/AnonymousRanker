package com.anonranker.service.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String token) {
        super("Session not found for token: " + token);
    }
}
