package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record WaitingListRequestDTO(
        UUID patientUuid,
        UUID professionalUuid,
        AppointmentType preferredType,
        LocalDateTime preferredDate
) {}