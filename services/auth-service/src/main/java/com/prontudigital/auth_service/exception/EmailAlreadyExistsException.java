package com.prontudigital.auth_service.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email '"+ email + "' já está em uso.");
    }
}
