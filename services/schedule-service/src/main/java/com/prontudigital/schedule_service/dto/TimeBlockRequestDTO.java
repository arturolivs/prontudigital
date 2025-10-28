package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.TimeBlockType;

import java.time.LocalDateTime;

public record TimeBlockRequestDTO(
        Long professionalId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String reason,
        TimeBlockType type
) {}