package com.prontudigital.schedule.repository;

import com.prontudigital.schedule.enums.AppointmentStatus;
import com.prontudigital.schedule.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByProfessionalIdAndStartDateTimeBetween(
            Long professionalId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByPatientIdAndStartDateTimeBetween(
            Long patientId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByStatusAndStartDateTimeBetween(
            AppointmentStatus status, LocalDateTime start, LocalDateTime end);

    boolean existsByProfessionalIdAndStartDateTimeBetweenAndStatusNot(
            Long professionalId, LocalDateTime start, LocalDateTime end, AppointmentStatus status);
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);
}