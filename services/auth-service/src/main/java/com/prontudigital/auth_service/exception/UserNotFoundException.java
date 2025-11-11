package com.prontudigital.auth_service.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Usuário não encontrado com ID: " + id);
    }
    public UserNotFoundException(UUID uuid) {
        super("Usuário não encontrado com UUID: " + uuid);
    }
}
