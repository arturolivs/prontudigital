package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.enums.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponseDTO(
        Long id,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        UUID professionalUuid,
        UUID patientUuid,
        AppointmentType type,
        AppointmentStatus status,
        String notes,
        LocalDateTime createdAt,
        Long evaluationId,
        LocalDateTime completedAt
) {}