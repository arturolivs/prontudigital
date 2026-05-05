package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Visualizacao do agendamento com nomes resolvidos")
public record AgendamentoViewDTO(

        Long id,
        LocalDateTime inicioEm,
        LocalDateTime fimEm,
        UUID profissionalUuid,
        UUID pacienteUuid,
        TipoAgendamento tipo,
        StatusAgendamento status,
        String nomePaciente,
        String nomeProfissional,
        Long avaliacaoId

) {}