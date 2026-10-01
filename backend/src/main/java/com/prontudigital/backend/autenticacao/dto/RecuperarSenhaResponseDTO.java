package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta das operações de recuperação de senha")
public record RecuperarSenhaResponseDTO(

        @Schema(description = "Mensagem descrevendo o resultado da operação")
        String mensagem

) {}
