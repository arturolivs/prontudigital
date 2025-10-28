package com.prontudigital.schedule_service.dto;

import java.time.LocalDateTime;

public record RescheduleRequestDTO(
        Long appointmentId,
        LocalDateTime newStartDateTime,
        LocalDateTime newEndDateTime,
        Long patientId
) {}