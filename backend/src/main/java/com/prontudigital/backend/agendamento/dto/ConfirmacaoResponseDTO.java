package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta da ação de confirmação ou recusa de agendamento")
public record ConfirmacaoResponseDTO(

        @Schema(description = "Mensagem descrevendo o resultado da ação")
        String mensagem
) {}
