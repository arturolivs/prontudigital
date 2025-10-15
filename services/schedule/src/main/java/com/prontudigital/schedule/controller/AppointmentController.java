package com.prontudigital.schedule.controller;

import com.prontudigital.schedule.dto.AppointmentResponseDTO;
import com.prontudigital.schedule.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agenda")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "day") String view) {

        if (date != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsWithUserDetailsByDate(date));
        } else if (start != null && end != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsWithUserDetailsByDateRange(start, end));
        } else {
            // If no date provided, return today's appointments
            return ResponseEntity.ok(appointmentService.getAppointmentsWithUserDetailsByDate(LocalDate.now()));
        }
    }

}