package com.prontudigital.schedule.dto;

import com.prontudigital.schedule.enums.AppointmentStatus;
import com.prontudigital.schedule.enums.AppointmentType;

import java.time.LocalDateTime;

public record AppointmentResponseDTO(
        Long id,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Long professionalId,
        Long patientId,
        AppointmentType type,
        AppointmentStatus status,
        String notes,
        LocalDateTime createdAt
) {}