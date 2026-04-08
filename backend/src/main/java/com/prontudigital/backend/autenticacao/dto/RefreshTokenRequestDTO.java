package com.prontudigital.backend.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(

        @NotBlank
        String refreshToken

) {}