package com.prontudigital.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;

@Schema(description = "Resposta de autenticação com tokens JWT")
public record JwtResponseDTO(
        @Schema(description = "Token de acesso JWT", example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken,

        @Schema(description = "Token de refresh", example = "eyJhbGciOiJIUzI1NiIs...")
        String refreshToken,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,

        @Schema(description = "Tempo de expiração em segundos", example = "900")
        Long expiresIn,

        @Schema(description = "Nome de usuário", example = "joaosilva")
        String username,

        @Schema(description = "Roles do usuário", example = "[\"USER\"]")
        Collection<String> roles
) {
    public JwtResponseDTO(String accessToken, String refreshToken, String username, Collection<String> roles) {
        this(accessToken, refreshToken, "Bearer", 900L, username, roles); // 15min
    }
}