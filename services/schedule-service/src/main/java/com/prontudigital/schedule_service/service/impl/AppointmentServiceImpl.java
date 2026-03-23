package com.prontudigital.schedule_service.service.impl;

import com.prontudigital.schedule_service.client.UserServiceClient;
import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import com.prontudigital.schedule_service.entity.Appointment;
import com.prontudigital.schedule_service.entity.TimeBlock;
import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.enums.AppointmentType;
import com.prontudigital.schedule_service.exception.*;
import com.prontudigital.schedule_service.repository.AppointmentRepository;
import com.prontudigital.schedule_service.repository.TimeBlockRepository;
import com.prontudigital.schedule_service.service.AppointmentService;
import com.prontudigital.schedule_service.service.validation.UserValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final UserServiceClient userServiceClient;
    private final UserValidationService userValidationService;

    // ========== CONVERSÕES ==========

    private AppointmentResponseDTO convertToDTO(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getProfessionalUuid(),
                appointment.getPatientUuid(),
                appointment.getType(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt(),
                appointment.getEvaluation() != null ? appointment.getEvaluation().getId() : null,
                appointment.getCompletedAt()
        );
    }

    private AppointmentViewDTO convertToViewDTO(Appointment appointment) {
        try {
            UserInfoDTO patient = userServiceClient.getUserByUuid(appointment.getPatientUuid());
            UserInfoDTO professional = userServiceClient.getUserByUuid(appointment.getProfessionalUuid());

            String patientName = patient != null ? patient.getFullName() : "Paciente não encontrado";
            String professionalName = professional != null ? professional.getFullName() : "Profissional não encontrado";

            return new AppointmentViewDTO(
                    appointment.getId(),
                    appointment.getStartDateTime(),
                    appointment.getEndDateTime(),
                    appointment.getProfessionalUuid(),
                    appointment.getPatientUuid(),
                    appointment.getType(),
                    appointment.getStatus(),
                    patientName,
                    professionalName,
                    appointment.getEvaluation() != null ? appointment.getEvaluation().getId() : null
            );
        } catch (Exception e) {
            log.error("Erro ao buscar dados de usuário", e);
            return new AppointmentViewDTO(
                    appointment.getId(),
                    appointment.getStartDateTime(),
                    appointment.getEndDateTime(),
                    appointment.getProfessionalUuid(),
                    appointment.getPatientUuid(),
                    appointment.getType(),
                    appointment.getStatus(),
                    "Indisponível",
                    "Indisponível",
                    appointment.getEvaluation() != null ? appointment.getEvaluation().getId() : null
            );
        }
    }

    // ========== PERMISSÕES ==========

    private void validateSchedulePermission(AppointmentRequestDTO request, UUID currentUserUuid, String currentUserRole) {
        if ("ADMIN".equals(currentUserRole)) {
            return;
        }
        if ("PATIENT".equals(currentUserRole)) {
            if (!request.patientUuid().equals(currentUserUuid)) {
                throw new UnauthorizedException("Paciente só pode criar agendamentos para si mesmo");
            }
            return;
        }
        if ("PROFESSIONAL".equals(currentUserRole)) {
            if (!request.professionalUuid().equals(currentUserUuid)) {
                throw new UnauthorizedException("Profissional só pode criar agendamentos para si mesmo");
            }
            return;
        }
        throw new UnauthorizedException("Usuário não autorizado a criar agendamentos");
    }

    private boolean hasPermissionToModify(Appointment appointment, UUID currentUserUuid, String currentUserRole) {
        if ("ADMIN".equals(currentUserRole)) {
            return true;
        }
        if ("PATIENT".equals(currentUserRole)) {
            return appointment.getPatientUuid().equals(currentUserUuid);
        }
        if ("PROFESSIONAL".equals(currentUserRole)) {
            return appointment.getProfessionalUuid().equals(currentUserUuid);
        }
        return false;
    }

    private boolean hasPermissionToView(Appointment appointment, UUID currentUserUuid, String currentUserRole) {
        if ("ADMIN".equals(currentUserRole)) {
            return true;
        }
        if ("PATIENT".equals(currentUserRole)) {
            return appointment.getPatientUuid().equals(currentUserUuid);
        }
        if ("PROFESSIONAL".equals(currentUserRole)) {
            return appointment.getProfessionalUuid().equals(currentUserUuid);
        }
        return false;
    }

    // ========== VALIDAÇÕES DE DISPONIBILIDADE ==========

    private void validateFutureDateTime(LocalDateTime dateTime) {
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentTimeException("Não é possível agendar para datas/horários passados");
        }
    }

    private void validateProfessionalAvailability(UUID professionalUuid, LocalDateTime start, LocalDateTime end) {
        List<TimeBlock> timeBlocks = timeBlockRepository.findConflictingTimeBlocks(professionalUuid, start, end);
        if (!timeBlocks.isEmpty()) {
            TimeBlock conflict = timeBlocks.get(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            throw new TimeBlockConflictException(
                    String.format("Horário indisponível. Profissional possui bloco de tempo conflitante: %s às %s. Motivo: %s",
                            conflict.getStartDateTime().format(formatter),
                            conflict.getEndDateTime().format(formatter),
                            conflict.getReason())
            );
        }

        List<Appointment> professionalConflicts = appointmentRepository
                .findConflictingAppointmentsForProfessional(professionalUuid, start, end);
        if (!professionalConflicts.isEmpty()) {
            throw new ProfessionalNotAvailableException("Profissional já possui agendamento neste horário.");
        }
    }

    private void validatePatientAvailability(UUID patientUuid, LocalDateTime start, LocalDateTime end) {
        List<Appointment> patientConflicts = appointmentRepository
                .findConflictingAppointmentsForPatient(patientUuid, start, end);
        if (!patientConflicts.isEmpty()) {
            throw new PatientNotAvailableException(
                    "Paciente já possui agendamento neste horário. Agendamentos conflitantes: " + patientConflicts.size()
            );
        }
    }

    @Override
    @Transactional
    public AppointmentResponseDTO scheduleAppointment(AppointmentRequestDTO request) {
        log.info("Tentando agendar consulta para paciente {} com profissional {} no horário {}",
                request.patientUuid(), request.professionalUuid(), request.startDateTime());

        UserInfoDTO user = userServiceClient.getCurrentUser();
        // 1. Permissão
        validateSchedulePermission(request,user.getUuid(), user.getRoles().getFirst());

        // 2. Validações básicas
        userValidationService.validateUserExists(request.patientUuid());
        userValidationService.validateUserExists(request.professionalUuid());
        validateFutureDateTime(request.startDateTime());

        // 3. Validação de disponibilidade
        validateProfessionalAvailability(request.professionalUuid(), request.startDateTime(), request.endDateTime());
        validatePatientAvailability(request.patientUuid(), request.startDateTime(), request.endDateTime());

        // 4. Validação da regra de negócio Avaliação/Tratamento
        if (request.type() == AppointmentType.TRATAMENTO) {
            if (request.evaluationId() == null) {
                throw new InvalidAppointmentRequestException("Tratamento deve estar associado a uma avaliação");
            }
            Appointment evaluation = appointmentRepository.findById(request.evaluationId())
                    .orElseThrow(() -> new EvaluationNotFoundException("Avaliação não encontrada"));
            if (evaluation.getType() != AppointmentType.AVALIACAO) {
                throw new InvalidAppointmentRequestException("O appointment referenciado não é uma avaliação");
            }
            if (!evaluation.getPatientUuid().equals(request.patientUuid())) {
                throw new InvalidAppointmentRequestException("O paciente do tratamento deve ser o mesmo da avaliação");
            }
            if (evaluation.getStatus() != AppointmentStatus.COMPLETED) {
                throw new InvalidAppointmentRequestException("A avaliação deve estar concluída para agendar tratamentos");
            }
            // Opcional: verificar se o profissional é o mesmo? (não exigido pela regra)
        } else if (request.type() == AppointmentType.AVALIACAO && request.evaluationId() != null) {
            throw new InvalidAppointmentRequestException("Avaliação não pode ter evaluationId");
        }

        // 5. Criação do agendamento
        Appointment appointment = Appointment.builder()
                .patientUuid(request.patientUuid())
                .professionalUuid(request.professionalUuid())
                .notes(request.notes())
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .status(AppointmentStatus.SCHEDULED)
                .type(request.type())
                .build();

        // Associa a avaliação se for tratamento
        if (request.type() == AppointmentType.TRATAMENTO && request.evaluationId() != null) {
            appointment.setEvaluation(appointmentRepository.getReferenceById(request.evaluationId()));
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Consulta agendada com sucesso. ID: {}", savedAppointment.getId());

        return convertToDTO(savedAppointment);
    }

    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Agendamento não encontrado"));
        UserInfoDTO user = userServiceClient.getCurrentUser();

        if (!hasPermissionToModify(appointment, user.getUuid(), user.getRoles().getFirst())) {
            throw new UnauthorizedException("Usuário não autorizado a cancelar este agendamento");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentAlreadyCancelledException("Agendamento já está cancelado");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentStateException("Agendamento concluído não pode ser cancelado");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment cancelled = appointmentRepository.save(appointment);
        convertToDTO(cancelled);
    }

    @Override
    public List<AppointmentViewDTO> viewAppointments(LocalDate date,
                                                     String viewType) {
        UserInfoDTO user = userServiceClient.getCurrentUser();
        // Validação de permissão
        if ("PATIENT".equals(user.getRoles().getFirst())) {
            throw new UnauthorizedException("Paciente não pode visualizar agenda de profissional");
        }
        if ("PROFESSIONAL".equals(user.getRoles().getFirst()) ) {
            throw new UnauthorizedException("Profissional só pode visualizar sua própria agenda");
        }
        // ADMIN pode qualquer

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
                .findByProfessionalUuidAndStartDateTimeBetween(user.getUuid(), startDateTime, endDateTime);

        return appointments.stream()
                .map(this::convertToViewDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentViewDTO> getTreatmentsByEvaluation(Long evaluationId) {
        Appointment evaluation = appointmentRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException("Avaliação não encontrada"));

        UserInfoDTO user = userServiceClient.getCurrentUser();

        if (!hasPermissionToView(evaluation, user.getUuid(), user.getRoles().getFirst())) {
            throw new UnauthorizedException("Usuário não autorizado a ver tratamentos desta avaliação");
        }

        List<Appointment> treatments = appointmentRepository.findByEvaluationId(evaluationId);
        return treatments.stream().map(this::convertToViewDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void completeAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Agendamento não encontrado"));

        UserInfoDTO user = userServiceClient.getCurrentUser();

        if (!hasPermissionToModify(appointment, user.getUuid(), user.getRoles().getFirst())) {
            throw new UnauthorizedException("Usuário não autorizado a concluir este agendamento");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentAlreadyCompletedException("Agendamento já está concluído");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidAppointmentStateException("Agendamento cancelado não pode ser concluído");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }
}