package com.prontudigital.auth_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserNameAlreadyExistsException extends RuntimeException {
    public UserNameAlreadyExistsException(String username) {
        super("Usuário '" + username + "' já existe.");
    }
}
