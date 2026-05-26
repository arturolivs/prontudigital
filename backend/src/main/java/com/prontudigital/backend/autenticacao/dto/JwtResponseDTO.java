package com.prontudigital.backend.autenticacao.dto;


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

        @Schema(description = "Perfis do usuário", example = "[\"ENFERMEIRO\"]")
        Collection<String> perfis

) {

    public JwtResponseDTO(String accessToken, String refreshToken,
                          String username, Collection<String> perfis) {
        this(accessToken, refreshToken, "Bearer", 900L, username, perfis);
    }
}