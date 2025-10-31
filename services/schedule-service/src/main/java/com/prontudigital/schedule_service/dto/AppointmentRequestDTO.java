package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRequestDTO (
        @NotNull UUID professionalUuid,
        @NotNull UUID patientUuid,
        String notes,
        @NotNull LocalDateTime startDateTime,
        @NotNull LocalDateTime endDateTime,
        @NotNull AppointmentType type
) {}