package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.AppointmentType;

import java.time.LocalDateTime;

public record WaitingListRequestDTO(
        Long patientId,
        Long professionalId,
        AppointmentType preferredType,
        LocalDateTime preferredDate
) {}