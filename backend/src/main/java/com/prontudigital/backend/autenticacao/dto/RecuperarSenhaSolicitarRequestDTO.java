package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitação de código de recuperação de senha via WhatsApp")
public record RecuperarSenhaSolicitarRequestDTO(

        @Schema(description = "Telefone cadastrado do usuário (com DDD)", example = "11999999999")
        @NotBlank(message = "O telefone é obrigatório")
        String telefone

) {}
