package com.anonranker.service.exception;

public class AlreadyVotedException extends RuntimeException {
    public AlreadyVotedException() {
        super("This member has already submitted a ballot for this topic.");
    }
}
