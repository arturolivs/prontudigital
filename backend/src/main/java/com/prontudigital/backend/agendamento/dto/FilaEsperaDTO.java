package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import java.time.LocalDateTime;
import java.util.UUID;

public record FilaEsperaDTO(
        Long id,
        UUID uuid,
        UUID pacienteUuid,
        UUID profissionalUuid,
        TipoAgendamento tipoPreferido,
        LocalDateTime dataPreferida,
        Integer prioridade,
        StatusFilaEspera status,
        LocalDateTime createdAt
) {}