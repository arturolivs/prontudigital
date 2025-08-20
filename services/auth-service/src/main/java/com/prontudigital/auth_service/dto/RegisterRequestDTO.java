package com.prontudigital.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String fullName,

        @Email
        String email,

        @NotBlank(message = "Usuário é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 30, message = "Senha deve ter entre 8 e 30 caracteres")
        String password,

        Set<String> roles
) {
    public RegisterRequestDTO {
        if (roles == null || roles.isEmpty()) {
            roles = Set.of("USER");
        }
    }
}