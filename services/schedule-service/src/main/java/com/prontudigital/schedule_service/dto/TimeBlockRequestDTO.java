package com.prontudigital.schedule_service.dto;

import com.prontudigital.schedule_service.enums.TimeBlockType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimeBlockRequestDTO(
        UUID professionalUuid,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String reason,
        TimeBlockType type
) {}