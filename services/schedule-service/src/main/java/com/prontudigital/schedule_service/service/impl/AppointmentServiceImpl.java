package com.prontudigital.schedule_service.service.impl;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.exception.AppointmentAlreadyCancelledException;
import com.prontudigital.schedule_service.exception.AppointmentNotFoundException;
import com.prontudigital.schedule_service.exception.InvalidViewTypeException;
import com.prontudigital.schedule_service.model.Appointment;
import com.prontudigital.schedule_service.repository.AppointmentRepository;
import com.prontudigital.schedule_service.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;

    private AppointmentResponseDTO convertToDTO(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getProfessionalId(),
                appointment.getPatientId(),
                appointment.getType(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt()
        );
    }

    private AppointmentViewDTO convertToViewDTO(Appointment appointment) {
        String patientName = "patientName";//patientServiceClient.getPatientName(appointment.getPatientId());
        String professionalName = "professionalName";//professionalServiceClient.getProfessionalName(appointment.getProfessionalId());

        return new AppointmentViewDTO(
                appointment.getId(),
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getProfessionalId(),
                appointment.getPatientId(),
                appointment.getType(),
                appointment.getStatus(),
                patientName,
                professionalName
        );
    }

    @Override
    public AppointmentResponseDTO scheduleAppointment(AppointmentRequestDTO request) {

        Appointment appointment = Appointment.builder()
                .patientId(request.patientId())
                .professionalId(request.professionalId())
                .notes(request.notes())
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .status(AppointmentStatus.SCHEDULED)
                .type(request.type())
                .createdAt(LocalDateTime.now())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return convertToDTO(savedAppointment);
    }

    public AppointmentResponseDTO cancelAppointment(Long appointmentId, Long patientId)  {
        Appointment appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new AppointmentNotFoundException("Agendamento não encontrado"));


        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentAlreadyCancelledException("Agendamento já está cancelado");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment cancelledAppointment = appointmentRepository.save(appointment);

        return convertToDTO(cancelledAppointment);
    }

    @Override
    public List<AppointmentViewDTO> viewAppointments(Long professionalId, LocalDate date, String viewType) {
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
                .findByProfessionalIdAndStartDateTimeBetween(professionalId, startDateTime, endDateTime);

        return appointments.stream()
                .map(this::convertToViewDTO)
                .collect(Collectors.toList());
    }
}