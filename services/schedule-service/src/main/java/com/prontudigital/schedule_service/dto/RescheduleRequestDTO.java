package com.prontudigital.schedule_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RescheduleRequestDTO(
        Long appointmentId,
        LocalDateTime newStartDateTime,
        LocalDateTime newEndDateTime,
        UUID patientUuid
) {}