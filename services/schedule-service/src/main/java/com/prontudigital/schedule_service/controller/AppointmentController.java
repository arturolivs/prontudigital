package com.prontudigital.schedule_service.controller;

import com.prontudigital.schedule_service.config.doc.AppointmentApiResponses;
import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Agendamentos", description = "Endpoints para gerenciamento de consultas e agendamentos")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/schedule")
    @Operation(
            summary = "Agendar consulta",
            description = "Cria um novo agendamento de consulta. Verifica disponibilidade de horário."
    )
    @AppointmentApiResponses.ScheduleSuccessResponse
    @AppointmentApiResponses.StandardAppointmentResponses
    public ResponseEntity<AppointmentResponseDTO> scheduleAppointment(
            @Valid @RequestBody AppointmentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.scheduleAppointment(request));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancelar consulta",
            description = "Cancela uma consulta existente. Requer o UUID do paciente para validação."
    )
    @AppointmentApiResponses.CancelSuccessResponse
    @AppointmentApiResponses.StandardAppointmentResponses
    @ApiResponse(responseCode = "400", description = "ID ou UUID inválido")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @Parameter(description = "ID da consulta", example = "1", required = true)
            @PathVariable Long id,

            @Parameter(description = "UUID do paciente (para validação)",
                    example = "123e4567-e89b-12d3-a456-426614174001", required = true)
            @RequestParam UUID patientUuid) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, patientUuid));
    }

    @GetMapping("/view")
    @Operation(
            summary = "Visualizar agenda",
            description = "Retorna a agenda de um profissional para um determinado dia, semana ou mês."
    )
    @AppointmentApiResponses.ViewSuccessResponse
    @AppointmentApiResponses.StandardAppointmentResponses
    public ResponseEntity<List<AppointmentViewDTO>> viewAppointments(
            @Parameter(description = "UUID do profissional",
                    example = "66dee742-717f-4f22-9f79-74779f5866f7", required = true)
            @RequestParam UUID professionalUuid,

            @Parameter(description = "Data base para visualização (padrão: hoje)",
                    example = "2026-02-26")
            @RequestParam(required = false) LocalDate date,

            @Parameter(description = "Tipo de visualização: day, week, month (padrão: day)",
                    example = "week",
                    schema = @Schema(allowableValues = {"day", "week", "month"}))
            @RequestParam(required = false) String viewType) {

        if (date == null) {
            date = LocalDate.now();
        }
        if (viewType == null) {
            viewType = "day";
        }

        return ResponseEntity.ok(appointmentService.viewAppointments(professionalUuid, date, viewType));
    }
}