package com.prontudigital.auth_service.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "Usuário é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 30, message = "Senha deve ter entre 8 e 30 caracteres")
        String password
) {}