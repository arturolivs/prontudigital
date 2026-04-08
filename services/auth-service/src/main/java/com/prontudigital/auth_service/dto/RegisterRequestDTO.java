package com.prontudigital.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Dados para registro de novo usuário")
public record RegisterRequestDTO(
        @Schema(description = "Nome completo", example = "João Silva")
        @NotBlank String fullName,

        @Schema(description = "E-mail", example = "joao@email.com")
        @Email String email,

        @Schema(description = "Nome de usuário", example = "joaosilva")
        @NotBlank String username,

        @Schema(description = "Senha (mínimo 8 caracteres)", example = "senha123")
        @NotBlank @Size(min = 8, max = 30) String password,

        @Schema(description = "Lista de roles (padrão: USER)", example = "[\"USER\", \"ADMIN\"]")
        Set<String> roles
) {
}