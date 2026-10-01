package com.prontudigital.backend.autenticacao.dto;

public record RefreshTokenResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {}