package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Requisicao para remarcar agendamento")
public record RemarcarAgendamentoRequestDTO(

        @NotNull Long agendamentoId,
        @NotNull LocalDateTime novoInicioEm,
        @NotNull LocalDateTime novoFimEm,
        @NotNull UUID pacienteUuid

) {}