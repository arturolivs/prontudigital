package com.prontudigital.schedule_service.exception;

public class AppointmentAlreadyCompletedException extends RuntimeException {
    public AppointmentAlreadyCompletedException(String message) { super(message); }
}