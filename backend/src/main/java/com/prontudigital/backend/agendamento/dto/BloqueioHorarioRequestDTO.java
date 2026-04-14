package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Requisicao para bloqueio de horario do profissional")
public record BloqueioHorarioRequestDTO(

        @NotNull UUID profissionalUuid,
        @NotNull LocalDateTime inicioEm,
        @NotNull LocalDateTime fimEm,
        String motivo,
        TipoBloqueio tipo

) {}