package com.prontudigital.schedule_service.repository;

import com.prontudigital.schedule_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByIdAndPatientUuid(Long id, UUID patientUuid);

    List<Appointment> findByProfessionalUuidAndStartDateTimeBetween(
            UUID professionalUuid, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.professionalUuid = :professionalUuid AND " +
            "a.status NOT IN ('CANCELLED') AND " +
            "((a.startDateTime < :end AND a.endDateTime > :start))")
    List<Appointment> findConflictingAppointmentsForProfessional(
            @Param("professionalUuid") UUID professionalUuid,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.patientUuid = :patientUuid AND " +
            "a.status NOT IN ('CANCELLED') AND " +
            "((a.startDateTime < :end AND a.endDateTime > :start))")
    List<Appointment> findConflictingAppointmentsForPatient(
            @Param("patientUuid") UUID patientUuid,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}