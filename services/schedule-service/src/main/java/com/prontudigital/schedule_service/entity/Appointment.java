package com.prontudigital.schedule_service.entity;

import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.enums.AppointmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @Column(name = "patient_uuid")
    private UUID patientUuid;

    @Column(name = "professional_uuid")
    private UUID professionalUuid;

    private String notes;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    private AppointmentType type;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}