package com.prontudigital.schedule_service.service;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {
  AppointmentResponseDTO scheduleAppointment(AppointmentRequestDTO request);
  AppointmentResponseDTO cancelAppointment(Long appointmentId);
  List<AppointmentViewDTO> viewAppointments(UUID professionalUuid,
                                            LocalDate date,
                                            String viewType);
  List<AppointmentViewDTO> getTreatmentsByEvaluation(Long evaluationId);
  AppointmentResponseDTO completeAppointment(Long appointmentId);

}
