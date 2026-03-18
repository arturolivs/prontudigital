package com.prontudigital.schedule_service.controller;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> schedule(@RequestBody @Valid AppointmentRequestDTO request) {
        AppointmentResponseDTO response = appointmentService.scheduleAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AppointmentViewDTO>> viewAppointments(
            @RequestParam UUID professionalUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String viewType) {
        List<AppointmentViewDTO> appointments = appointmentService.viewAppointments(
                professionalUuid, date, viewType);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/evaluation/{evaluationId}/treatments")
    public ResponseEntity<List<AppointmentViewDTO>> getTreatmentsByEvaluation(@PathVariable Long evaluationId) {
        List<AppointmentViewDTO> treatments = appointmentService.getTreatmentsByEvaluation(
                evaluationId);
        return ResponseEntity.ok(treatments);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponseDTO> complete(@PathVariable Long id) {
        AppointmentResponseDTO response = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(response);
    }
}