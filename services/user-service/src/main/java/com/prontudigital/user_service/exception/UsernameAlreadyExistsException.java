package com.prontudigital.user_service.exception;

public class UserNameAlreadyExistsException extends RuntimeException {
    public UserNameAlreadyExistsException(String username) {
        super("Usuário já existe: " + username);
    }
}
