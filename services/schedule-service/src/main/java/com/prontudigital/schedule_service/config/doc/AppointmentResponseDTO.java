package com.prontudigital.schedule_service.config.doc;

import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.enums.AppointmentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resposta com dados da consulta agendada")
public record AppointmentResponseDTO(

        @Schema(description = "ID da consulta", example = "1")
        Long id,

        @Schema(description = "Data e hora de início", example = "2026-02-26T14:00:00")
        LocalDateTime startDateTime,

        @Schema(description = "Data e hora de término", example = "2026-02-26T14:30:00")
        LocalDateTime endDateTime,

        @Schema(description = "UUID do profissional", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID professionalUuid,

        @Schema(description = "UUID do paciente", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID patientUuid,

        @Schema(description = "Tipo de consulta", example = "CONSULTA")
        AppointmentType type,

        @Schema(description = "Status da consulta", example = "SCHEDULED")
        AppointmentStatus status,

        @Schema(description = "Observações", example = "Paciente com sintomas de gripe")
        String notes,

        @Schema(description = "Data de criação do agendamento", example = "2026-02-25T10:00:00")
        LocalDateTime createdAt
) {}
