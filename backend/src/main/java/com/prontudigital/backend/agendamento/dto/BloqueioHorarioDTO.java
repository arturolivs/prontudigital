package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.TipoBloqueio;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record BloqueioHorarioDTO(
        Long id,
        UUID uuid,
        @NotNull UUID profissionalUuid,
        @NotNull LocalDateTime inicioEm,
        @NotNull LocalDateTime fimEm,
        String motivo,
        @NotNull TipoBloqueio tipo
) {}
