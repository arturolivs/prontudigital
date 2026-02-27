package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.AppointmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;


@Schema(description = "Requisição para agendamento de consulta")
public record AppointmentRequestDTO(

        @Schema(description = "UUID do profissional",
                example = "123e4567-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID professionalUuid,

        @Schema(description = "UUID do paciente",
                example = "123e4567-e89b-12d3-a456-426614174001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID patientUuid,

        @Schema(description = "Observações sobre a consulta",
                example = "Paciente com sintomas de gripe")
        String notes,

        @Schema(description = "Data e hora de início",
                example = "2026-02-26T14:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDateTime startDateTime,

        @Schema(description = "Data e hora de término",
                example = "2026-02-26T14:30:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDateTime endDateTime,

        @Schema(description = "Tipo de consulta",
                example = "CONSULTA",
                allowableValues = {"CONSULTA", "RETORNO", "EXAME", "CIRURGIA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull AppointmentType type
) {}