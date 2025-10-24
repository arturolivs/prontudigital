package com.prontudigital.schedule_service.service.impl;

import com.prontudigital.schedule_service.exception.AppointmentNotFoundException;
import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.model.Appointment;
import com.prontudigital.schedule_service.repository.AppointmentRepository;
import com.prontudigital.schedule_service.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        // Notificar paciente (RF09)
        //notifyPatient(savedAppointment, "AGENDAMENTO");

        return convertToDTO(savedAppointment);
    }

    public List<AppointmentResponseDTO> getAgenda(Long professionalId, LocalDate date, String viewType) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        switch (viewType.toUpperCase()) {
            case "DAY":
                startDateTime = date.atStartOfDay();
                endDateTime = date.atTime(LocalTime.MAX);
                break;
            case "WEEK":
                startDateTime = date.atStartOfDay();
                endDateTime = date.plusDays(6).atTime(LocalTime.MAX);
                break;
            case "MONTH":
                startDateTime = date.withDayOfMonth(1).atStartOfDay();
                endDateTime = date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX);
                break;
            default:
                throw new IllegalArgumentException("Tipo de visualização inválido: " + viewType);
        }

        return appointmentRepository
                .findByProfessionalIdAndStartDateTimeBetween(professionalId, startDateTime, endDateTime)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // RF10: Cancelamento e reagendamento
    public AppointmentResponseDTO cancelAppointment(Long appointmentId, Long patientId)  {
        Appointment appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new AppointmentNotFoundException("Agendamento não encontrado"));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment cancelledAppointment = appointmentRepository.save(appointment);

        // Notificar paciente
        //notifyPatient(cancelledAppointment, "CANCELAMENTO");

        // Verificar lista de espera (RF12)
      //  checkWaitList(appointment);

        return convertToDTO(cancelledAppointment);
    }
}