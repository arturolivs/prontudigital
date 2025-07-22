package com.prontudigital.user_service.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Username já existe: " + username);
    }
}
