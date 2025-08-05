package com.prontudigital.auth_service.exception;

public class UserNameAlreadyExistsException extends RuntimeException {
    public UserNameAlreadyExistsException(String username) {
        super("Usuário já existe: " + username);
    }
}
