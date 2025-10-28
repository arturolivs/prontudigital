package com.prontudigital.schedule_service.model;

import com.prontudigital.schedule_service.enums.TimeBlockType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    private Long professionalId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String reason;

    @Enumerated(EnumType.STRING)
    private TimeBlockType type;

    @CreationTimestamp
    private LocalDateTime createdAt;
}