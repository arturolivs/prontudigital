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
import java.util.UUID;

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
            @RequestParam UUID patientUuid) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, patientUuid));
    }
    @GetMapping("/view")
    public ResponseEntity<List<AppointmentViewDTO>> viewAppointments(
            @RequestParam UUID professionalUuid,
            @RequestParam(required = false) LocalDate date,
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