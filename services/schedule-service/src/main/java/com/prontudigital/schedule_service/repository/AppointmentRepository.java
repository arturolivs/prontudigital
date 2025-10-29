package com.prontudigital.schedule_service.repository;

import com.prontudigital.schedule_service.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);

    List<Appointment> findByProfessionalIdAndStartDateTimeBetween(
            Long professionalId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.professionalId = :professionalId AND " +
            "a.status NOT IN ('CANCELLED') AND " +
            "((a.startDateTime < :end AND a.endDateTime > :start))")
    List<Appointment> findConflictingAppointmentsForProfessional(
            @Param("professionalId") Long professionalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.patientId = :patientId AND " +
            "a.status NOT IN ('CANCELLED') AND " +
            "((a.startDateTime < :end AND a.endDateTime > :start))")
    List<Appointment> findConflictingAppointmentsForPatient(
            @Param("patientId") Long patientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}