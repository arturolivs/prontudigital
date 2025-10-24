package com.prontudigital.schedule_service.service;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;

public interface AppointmentService {
    AppointmentResponseDTO scheduleAppointment(AppointmentRequestDTO request);
}
