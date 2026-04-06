package com.prontudigital.schedule_service.utils;

import com.prontudigital.schedule_service.client.UserServiceClient;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import com.prontudigital.schedule_service.entity.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentUtil {

    private final UserServiceClient userServiceClient;

    public static AppointmentResponseDTO convertToDTO(Appointment appointment) {
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

    public AppointmentViewDTO convertToViewDTO(Appointment appointment) {
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

}
