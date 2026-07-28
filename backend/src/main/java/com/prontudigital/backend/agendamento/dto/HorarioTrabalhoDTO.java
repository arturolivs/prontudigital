package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Janela semanal de atendimento do profissional (RF05)")
public record HorarioTrabalhoDTO(

        Long id,

        UUID uuid,

        @Schema(description = "UUID do profissional")
        @NotNull UUID profissionalUuid,

        @Schema(description = "ISO-8601: 1=Segunda ... 6=Sabado, 7=Domingo", example = "1")
        @NotNull @Min(1) @Max(7) Integer diaSemana,

        @Schema(description = "Inicio do expediente", example = "08:00")
        @NotNull LocalTime horaInicio,

        @Schema(description = "Fim do expediente", example = "12:00")
        @NotNull LocalTime horaFim,

        @Schema(description = "Janela ativa")
        Boolean ativo

) {}
