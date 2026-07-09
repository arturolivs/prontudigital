package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados do agendamento")
public record AgendamentoResponseDTO(

        Long id,
        LocalDateTime inicioEm,
        LocalDateTime fimEm,
        UUID profissionalUuid,
        UUID pacienteUuid,
        TipoAgendamento tipo,
        TipoProcedimento tipoProcedimento,
        LocalAtendimento localAtendimento,
        Boolean pacienteAcamado,
        StatusAgendamento status,
        LocalDateTime criadoEm,
        Long avaliacaoId,
        LocalDateTime concluidoEm

) {}
