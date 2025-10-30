package com.prontudigital.schedule_service.entity;

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

    @Column(name = "professional_id")
    private Long professionalId;

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