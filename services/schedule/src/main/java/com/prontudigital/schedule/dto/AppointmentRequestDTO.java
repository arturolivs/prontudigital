package com.prontudigital.schedule.dto;

import com.prontudigital.schedule.enums.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequestDTO (
        @NotNull LocalDateTime startDateTime,
        @NotNull LocalDateTime endDateTime,
        @NotNull Long professionalId,
        @NotNull Long patientId,
        @NotNull AppointmentType type,
        String notes
) {}