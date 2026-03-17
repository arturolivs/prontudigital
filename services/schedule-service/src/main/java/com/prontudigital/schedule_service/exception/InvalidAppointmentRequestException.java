package com.prontudigital.schedule_service.exception;

public class InvalidAppointmentRequestException extends RuntimeException {
    public InvalidAppointmentRequestException(String message) { super(message); }
}