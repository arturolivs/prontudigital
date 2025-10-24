package com.prontudigital.schedule_service.controller;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
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

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "day") String view) {
       return ResponseEntity.ok(List.of());
        /*e
        if (date != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsWithUserDetailsByDate(date));
        } else if (start != null && end != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsWithUserDetailsByDateRange(start, end));
        } else {
            // If no date provided, return today's appointments
            return ResponseEntity.ok(appointmentService.getAppointmentsWithUserDetailsByDate(LocalDate.now()));
        }
        */
    }

}