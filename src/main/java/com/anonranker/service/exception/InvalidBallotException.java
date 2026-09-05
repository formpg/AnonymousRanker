package com.anonranker.service.exception;

public class InvalidBallotException extends RuntimeException {
    public InvalidBallotException(String message) {
        super(message);
    }
}
