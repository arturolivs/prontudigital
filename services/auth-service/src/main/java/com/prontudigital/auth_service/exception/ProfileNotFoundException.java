package com.prontudigital.auth_service.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String profileName) {
        super("Perfil não encontrado : " + profileName);
    }
}
