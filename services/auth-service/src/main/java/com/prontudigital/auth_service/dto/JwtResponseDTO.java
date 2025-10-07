package com.prontudigital.auth_service.dto;

import java.util.Collection;

public record JwtResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String username,
        Collection<String> roles
) {
    public JwtResponseDTO(String accessToken, String refreshToken, String username, Collection<String> roles) {
        this(accessToken, refreshToken, "Bearer", 900L, username, roles); // 15min
    }
}