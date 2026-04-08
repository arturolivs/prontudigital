package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "Dados para registro de novo usuario")
public record RegisterRequestDTO(

        @Schema(description = "Nome completo", example = "Joao Silva")
        @NotBlank
        String nomeCompleto,

        @Schema(description = "E-mail", example = "joao@email.com")
        @Email
        @NotBlank
        String email,

        @Schema(description = "Nome de usuario", example = "joaosilva")
        @NotBlank
        String username,

        @Schema(description = "Senha (minimo 8 caracteres)", example = "senha123")
        @NotBlank
        @Size(min = 8, max = 30)
        String password,

        @Schema(description = "Roles do usuario (padrao: USER)", example = "[\"USER\", \"ADMIN\"]")
        Set<String> roles

) {}