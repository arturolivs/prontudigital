package com.prontudigital.auth_service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Usuário não encontrado com ID: " + id);
    }
}
