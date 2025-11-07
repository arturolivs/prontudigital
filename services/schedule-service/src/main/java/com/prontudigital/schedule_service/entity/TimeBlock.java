package com.prontudigital.schedule_service.entity;

import com.prontudigital.schedule_service.enums.TimeBlockType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "time_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "professional_uuid", columnDefinition = "UUID")
    private UUID professionalUuid;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    private TimeBlockType type;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}