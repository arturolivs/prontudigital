package com.prontudigital.schedule_service.exception;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(String message) {
        super(message);
    }

}
