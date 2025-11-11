package com.prontudigital.schedule_service.exception;

public class TimeBlockConflictException extends RuntimeException {
    public TimeBlockConflictException(String message) {
        super(message);
    }
}
