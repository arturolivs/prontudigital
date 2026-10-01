package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais para autenticação")
public record LoginRequestDTO(

        @Schema(description = "Nome de usuário", example = "joaosilva")
        @NotBlank
        String username,

        @Schema(description = "Senha", example = "senha123")
        @NotBlank
        String senha

) {}