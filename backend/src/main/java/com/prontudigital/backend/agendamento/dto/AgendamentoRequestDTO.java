package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Requisicao para criacao de agendamento")
public record AgendamentoRequestDTO(

        @Schema(description = "UUID do paciente")
        @NotNull UUID pacienteUuid,

        @Schema(description = "UUID do profissional")
        @NotNull UUID profissionalUuid,

        @Schema(description = "Data e hora de inicio", example = "2026-02-26T14:00:00")
        @NotNull LocalDateTime inicioEm,

        @Schema(description = "Data e hora de termino", example = "2026-02-26T14:30:00")
        @NotNull LocalDateTime fimEm,

        @Schema(description = "Tipo do agendamento", example = "AVALIACAO")
        @NotNull TipoAgendamento tipo,

        @Schema(description = "Tipo de procedimento: PODIATRIA ou TRATAMENTO_FERIDAS", example = "PODIATRIA")
        @NotNull TipoProcedimento tipoProcedimento,

        @Schema(description = "Local do atendimento: CLINICA ou RESIDENCIAL", example = "CLINICA")
        @NotNull LocalAtendimento localAtendimento,

        @Schema(description = "Indica se o paciente esta acamado", example = "false")
        @NotNull Boolean pacienteAcamado,

        @Schema(description = "ID da avaliacao de origem (obrigatorio apenas para TRATAMENTO)")
        Long avaliacaoId

) {}