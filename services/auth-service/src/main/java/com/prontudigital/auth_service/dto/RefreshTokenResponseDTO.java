package com.prontudigital.auth_service.dto;

public record RefreshTokenResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {}