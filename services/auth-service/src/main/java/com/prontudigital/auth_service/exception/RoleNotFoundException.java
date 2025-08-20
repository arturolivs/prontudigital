package com.prontudigital.auth_service.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String role) {
        super("Função não encontrada : " + role);
    }
}
