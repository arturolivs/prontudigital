package com.prontudigital.schedule_service.controller;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/schedule")
    public ResponseEntity<AppointmentResponseDTO> scheduleAppointment(
            @Valid @RequestBody AppointmentRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.scheduleAppointment(request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @PathVariable Long id,
            @RequestParam Long patientId) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, patientId));
    }
    @GetMapping("/view")
    public ResponseEntity<List<AppointmentViewDTO>> viewAppointments(
            @RequestParam Long professionalId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String viewType) {

        if (date == null) {
            date = LocalDate.now();
        }
        if (viewType == null) {
            viewType = "day";
        }

        return ResponseEntity.ok(appointmentService.viewAppointments(professionalId, date, viewType));
    }


}