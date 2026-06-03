package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.TipoBloqueio;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record BloqueioRecorrenteDTO(
        Long id,
        UUID uuid,
        @NotNull UUID profissionalUuid,
        /** ISO-8601: 1=Segunda … 6=Sábado, 7=Domingo */
        @NotNull @Min(1) @Max(7) Integer diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        String motivo,
        @NotNull TipoBloqueio tipo
) {}
