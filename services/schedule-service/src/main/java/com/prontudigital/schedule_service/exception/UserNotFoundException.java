package com.prontudigital.schedule_service.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID uuid) {
        super("Usuário não encontrado com UUID: " + uuid);
    }    public UserNotFoundException(String msg) {
        super(msg);
    }
}
