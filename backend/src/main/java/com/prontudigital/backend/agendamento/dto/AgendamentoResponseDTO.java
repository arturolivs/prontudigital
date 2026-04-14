package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados completos do agendamento")
public record AgendamentoResponseDTO(

        Long id,
        LocalDateTime inicioEm,
        LocalDateTime fimEm,
        UUID profissionalUuid,
        UUID pacienteUuid,
        TipoAgendamento tipo,
        StatusAgendamento status,
        String observacoes,
        LocalDateTime createdAt,
        Long avaliacaoId,
        LocalDateTime concluidoEm

) {}