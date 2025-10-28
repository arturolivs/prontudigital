package com.prontudigital.schedule_service.service.impl;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.exception.AppointmentAlreadyCancelledException;
import com.prontudigital.schedule_service.exception.AppointmentNotFoundException;
import com.prontudigital.schedule_service.model.Appointment;
import com.prontudigital.schedule_service.repository.AppointmentRepository;
import com.prontudigital.schedule_service.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}