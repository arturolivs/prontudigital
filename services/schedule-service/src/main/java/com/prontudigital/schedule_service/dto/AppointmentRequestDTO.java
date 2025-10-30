package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequestDTO (
        @NotNull Long professionalId,
        @NotNull Long patientId,
        String notes,
        @NotNull LocalDateTime startDateTime,
        @NotNull LocalDateTime endDateTime,
        @NotNull AppointmentType type
) {}