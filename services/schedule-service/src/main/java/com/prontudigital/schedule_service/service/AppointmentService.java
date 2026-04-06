package com.prontudigital.schedule_service.service;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
  AppointmentResponseDTO scheduleAppointment(AppointmentRequestDTO request);
  void cancelAppointment(Long appointmentId);
  List<AppointmentViewDTO> viewAppointments(LocalDate date,
                                            String viewType);
  List<AppointmentViewDTO> getTreatmentsByEvaluation(Long evaluationId);
  void completeAppointment(Long appointmentId);

}
