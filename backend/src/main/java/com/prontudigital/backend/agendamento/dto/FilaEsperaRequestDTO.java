package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Requisicao para entrada na fila de espera")
public record FilaEsperaRequestDTO(

        @NotNull UUID pacienteUuid,
        @NotNull UUID profissionalUuid,
        TipoAgendamento tipoPreferido,
        LocalDateTime dataPreferida

) {}