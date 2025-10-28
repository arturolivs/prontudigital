package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.enums.AppointmentType;

import java.time.LocalDateTime;

public record AppointmentViewDTO(
        Long id,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Long professionalId,
        Long patientId,
        AppointmentType type,
        AppointmentStatus status,
        String patientName,
        String professionalName
) {}