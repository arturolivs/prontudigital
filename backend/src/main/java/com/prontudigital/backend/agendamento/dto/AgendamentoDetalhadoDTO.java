package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados completos do agendamento com nomes resolvidos e observações")
public record AgendamentoDetalhadoDTO(

        @Schema(description = "ID interno do agendamento")
        Long id,

        @Schema(description = "Data e hora de início")
        LocalDateTime inicioEm,

        @Schema(description = "Data e hora de término")
        LocalDateTime fimEm,

        @Schema(description = "UUID do profissional")
        UUID profissionalUuid,

        @Schema(description = "UUID do paciente")
        UUID pacienteUuid,

        @Schema(description = "Tipo: AVALIACAO ou TRATAMENTO")
        TipoAgendamento tipo,

        @Schema(description = "Tipo de procedimento: PODIATRIA ou TRATAMENTO_FERIDAS")
        TipoProcedimento tipoProcedimento,

        @Schema(description = "Status atual do agendamento")
        StatusAgendamento status,

        @Schema(description = "Nome completo do paciente (resolvido)")
        String nomePaciente,

        @Schema(description = "Nome completo do profissional (resolvido)")
        String nomeProfissional,

        @Schema(description = "Observações clínicas registradas")
        String observacoes,

        @Schema(description = "Data de criação do registro")
        LocalDateTime criadoEm,

        @Schema(description = "ID da avaliação vinculada (apenas para TRATAMENTO)")
        Long avaliacaoId,

        @Schema(description = "Data e hora em que o agendamento foi concluído")
        LocalDateTime concluidoEm

) {}
