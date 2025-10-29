package com.prontudigital.schedule_service.exception;

public class PatientNotAvailableException extends RuntimeException {
    public PatientNotAvailableException(String message) {
        super(message);
    }
}
