package com.prontudigital.schedule_service.service;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentResponseDTO scheduleAppointment(AppointmentRequestDTO request);
    AppointmentResponseDTO cancelAppointment(Long appointmentId, Long patientId);
    List<AppointmentViewDTO> viewAppointments(Long professionalId, LocalDate date, String viewType);
}
