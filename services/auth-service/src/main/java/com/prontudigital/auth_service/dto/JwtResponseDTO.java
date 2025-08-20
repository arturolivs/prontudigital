package com.prontudigital.auth_service.dto;

import java.util.Collection;

public record JwtResponseDTO(
        String token,
        String type,
        String username,
        Collection<String> roles
) {
    public JwtResponseDTO(String token, String username, Collection<String> roles) {
        this(token, "Bearer", username, roles);
    }
}