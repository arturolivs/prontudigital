package com.prontudigital.schedule_service.service.impl;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.exception.*;
import com.prontudigital.schedule_service.entity.Appointment;
import com.prontudigital.schedule_service.entity.TimeBlock;
import com.prontudigital.schedule_service.repository.AppointmentRepository;
import com.prontudigital.schedule_service.repository.TimeBlockRepository;
import com.prontudigital.schedule_service.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeBlockRepository timeBlockRepository;

    public AppointmentResponseDTO convertToDTO(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getProfessionalUuid(),
                appointment.getPatientUuid(),
                appointment.getType(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt()
        );
    }

    public AppointmentViewDTO convertToViewDTO(Appointment appointment) {
        String patientName = "patientName";//patientServiceClient.getPatientName(appointment.getPatientId());
        String professionalName = "professionalName";//professionalServiceClient.getProfessionalName(appointment.getProfessionalId());

        return new AppointmentViewDTO(
                appointment.getId(),
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getProfessionalUuid(),
                appointment.getPatientUuid(),
                appointment.getType(),
                appointment.getStatus(),
                patientName,
                professionalName
        );
    }

    @Override
    @Transactional
    public AppointmentResponseDTO scheduleAppointment(AppointmentRequestDTO request) {
        log.info("Tentando agendar consulta para paciente {} com profissional {} no horário {}",
                request.patientUuid(), request.professionalUuid(), request.startDateTime());

        validateProfessionalExists(request.professionalUuid());
        validatePatientExists(request.patientUuid());
        validateFutureDateTime(request.startDateTime());
        validateProfessionalAvailability(request.professionalUuid(), request.startDateTime(), request.endDateTime());
        validatePatientAvailability(request.patientUuid(), request.startDateTime(), request.endDateTime());

        Appointment appointment = Appointment.builder()
                .patientUuid(request.patientUuid())
                .professionalUuid(request.professionalUuid())
                .notes(request.notes())
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .status(AppointmentStatus.SCHEDULED)
                .type(request.type())
                .createdAt(LocalDateTime.now())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        log.info("Consulta agendada com sucesso. ID: {}", savedAppointment.getId());

        return convertToDTO(savedAppointment);
    }

    public AppointmentResponseDTO cancelAppointment(Long appointmentId, UUID patientUuid)  {
        Appointment appointment = appointmentRepository.findByIdAndPatientUuid(appointmentId, patientUuid)
                .orElseThrow(() -> new AppointmentNotFoundException("Agendamento não encontrado"));


        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentAlreadyCancelledException("Agendamento já está cancelado");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment cancelledAppointment = appointmentRepository.save(appointment);

        return convertToDTO(cancelledAppointment);
    }

    @Override
    public List<AppointmentViewDTO> viewAppointments(UUID professionalUuid, LocalDate date, String viewType) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        switch (viewType.toLowerCase()) {
            case "day":
                startDateTime = date.atStartOfDay();
                endDateTime = date.atTime(23, 59, 59);
                break;
            case "week":
                startDateTime = date.atStartOfDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                endDateTime = startDateTime.plusDays(6).with(LocalTime.of(23, 59, 59));
                break;
            case "month":
                startDateTime = date.withDayOfMonth(1).atStartOfDay();
                endDateTime = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                break;
            default:
                throw new InvalidViewTypeException("Tipo de visualização inválido: " + viewType);
        }

        List<Appointment> appointments = appointmentRepository
                .findByProfessionalUuidAndStartDateTimeBetween(professionalUuid, startDateTime, endDateTime);

        return appointments.stream()
                .map(this::convertToViewDTO)
                .collect(Collectors.toList());
    }


    private void validateProfessionalExists(UUID professionalId) {
        /*
        try {
            ProfessionalDTO professional = professionalServiceClient.getProfessionalById(professionalId);
            if (professional == null || !professional.isActive()) {
                throw new ProfessionalNotFoundException("Profissional não encontrado ou inativo");
            }
        } catch (Exception e) {
            throw new ProfessionalNotFoundException("Erro ao validar profissional: " + e.getMessage());
        }

         */
    }

    private void validatePatientExists(UUID patientId) {
        /*
        try {
            PatientDTO patient = patientServiceClient.getPatientById(patientId);
            if (patient == null || !patient.isActive()) {
                throw new PatientNotFoundException("Paciente não encontrado ou inativo");
            }
        } catch (Exception e) {
            throw new PatientNotFoundException("Erro ao validar paciente: " + e.getMessage());
        }

         */
    }

    private void validateFutureDateTime(LocalDateTime dateTime) {
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentTimeException("Não é possível agendar para datas/horários passados");
        }
    }

    public void validateProfessionalAvailability(UUID professionalUuid, LocalDateTime start, LocalDateTime end) {

        List<TimeBlock> timeBlocks = timeBlockRepository
                .findConflictingTimeBlocks(professionalUuid, start, end);

        if (!timeBlocks.isEmpty()) {
            TimeBlock conflict = timeBlocks.get(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            throw new TimeBlockConflictException(
                    String.format(
                            "Horário indisponível. Profissional possui bloco de tempo conflitante: %s às %s. Motivo: %s",
                            conflict.getStartDateTime().format(formatter),
                            conflict.getEndDateTime().format(formatter),
                            conflict.getReason()
                    )
            );
        }

        List<Appointment> professionalConflicts = appointmentRepository
                .findConflictingAppointmentsForProfessional(professionalUuid, start, end);

        if (!professionalConflicts.isEmpty()) {
            throw new ProfessionalNotAvailableException(
                    "Profissional já possui agendamento neste horário."
            );
        }
    }

    public void validatePatientAvailability(UUID patientUuid, LocalDateTime start, LocalDateTime end) {
        List<Appointment> patientConflicts = appointmentRepository
                .findConflictingAppointmentsForPatient(patientUuid, start, end);

        if (!patientConflicts.isEmpty()) {
            throw new PatientNotAvailableException(
                    "Paciente já possui agendamento neste horário. " +
                            "Agendamentos conflitantes: " + patientConflicts.size()
            );
        }
    }
}