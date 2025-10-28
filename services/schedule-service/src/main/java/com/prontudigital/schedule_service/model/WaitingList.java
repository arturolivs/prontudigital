package com.prontudigital.schedule_service.model;

import com.prontudigital.schedule_service.enums.AppointmentType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "waiting_list")
public class WaitingList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private Long professionalId;
    private AppointmentType preferredType;
    private LocalDateTime preferredDate;
    private Integer priority;
    private WaitingListStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}