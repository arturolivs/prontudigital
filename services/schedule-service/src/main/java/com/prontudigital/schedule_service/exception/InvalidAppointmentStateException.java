package com.prontudigital.schedule_service.exception;

public class InvalidAppointmentStateException extends RuntimeException {
    public InvalidAppointmentStateException(String message) { super(message); }
}