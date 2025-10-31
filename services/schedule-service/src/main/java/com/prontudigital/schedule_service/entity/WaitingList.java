package com.prontudigital.schedule_service.entity;

import com.prontudigital.schedule_service.enums.AppointmentType;
import com.prontudigital.schedule_service.enums.WaitingListStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "waiting_list")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", columnDefinition = "UUID",unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "professional_id")
    private Long professionalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_type")
    private AppointmentType preferredType;

    @Column(name = "preferred_date")
    private LocalDateTime preferredDate;

    private Integer priority;

    @Enumerated(EnumType.STRING)
    private WaitingListStatus status;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}