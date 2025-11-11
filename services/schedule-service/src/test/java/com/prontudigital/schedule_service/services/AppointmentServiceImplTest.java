package com.prontudigital.schedule_service.services;

import com.prontudigital.schedule_service.dto.AppointmentRequestDTO;
import com.prontudigital.schedule_service.dto.AppointmentResponseDTO;
import com.prontudigital.schedule_service.dto.AppointmentViewDTO;
import com.prontudigital.schedule_service.entity.Appointment;
import com.prontudigital.schedule_service.entity.TimeBlock;
import com.prontudigital.schedule_service.enums.AppointmentStatus;
import com.prontudigital.schedule_service.enums.AppointmentType;
import com.prontudigital.schedule_service.enums.TimeBlockType;
import com.prontudigital.schedule_service.exception.*;
import com.prontudigital.schedule_service.repository.AppointmentRepository;
import com.prontudigital.schedule_service.repository.TimeBlockRepository;
import com.prontudigital.schedule_service.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private AppointmentRequestDTO validRequest;
    private LocalDateTime futureDateTime;

    @BeforeEach
    void setUp() {
        futureDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        validRequest = new AppointmentRequestDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Consulta de rotina",
                futureDateTime,
                futureDateTime.plusHours(1),
                AppointmentType.CONSULTATION
        );
    }

    private Appointment createAppointment(AppointmentType type) {
        return Appointment.builder()
                .id(1L)
                .patientUuid(UUID.randomUUID())
                .professionalUuid(UUID.randomUUID())
                .notes("Consulta de rotina")
                .startDateTime(futureDateTime)
                .endDateTime(futureDateTime.plusHours(1))
                .status(AppointmentStatus.SCHEDULED)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void scheduleAppointment_WithValidData_ShouldReturnAppointmentResponse() {

        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);

        when(appointmentRepository.findConflictingAppointmentsForProfessional(any(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.findConflictingAppointmentsForPatient(any(), any(), any()))
                .thenReturn(List.of());
        when(timeBlockRepository.findConflictingTimeBlocks(any(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(appointment);

        AppointmentResponseDTO result = appointmentService.scheduleAppointment(validRequest);

        assertNotNull(result);
        assertEquals(appointment.getId(), result.id());
        assertEquals(appointment.getStartDateTime(), result.startDateTime());
        assertEquals(appointment.getEndDateTime(), result.endDateTime());
        assertEquals(appointment.getProfessionalUuid(), result.professionalUuid());
        assertEquals(appointment.getPatientUuid(), result.patientUuid());
        assertEquals(appointment.getType(), result.type());
        assertEquals(appointment.getStatus(), result.status());

        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void scheduleAppointment_WithPastDateTime_ShouldThrowException() {

        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
        AppointmentRequestDTO invalidRequest = new AppointmentRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Consulta", pastDateTime, pastDateTime.plusHours(1), AppointmentType.CONSULTATION
        );

        assertThrows(InvalidAppointmentTimeException.class,
                () -> appointmentService.scheduleAppointment(invalidRequest));

        verifyNoInteractions(appointmentRepository, timeBlockRepository);
    }

    @Test
    void scheduleAppointment_WithProfessionalConflict_ShouldThrowException() {
        Appointment conflictingAppointment = createAppointment(AppointmentType.CONSULTATION);

        when(appointmentRepository.findConflictingAppointmentsForProfessional(any(), any(), any()))
                .thenReturn(List.of(conflictingAppointment));

        assertThrows(ProfessionalNotAvailableException.class,
                () -> appointmentService.scheduleAppointment(validRequest));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void scheduleAppointment_WithPatientConflict_ShouldThrowException() {

        when(appointmentRepository.findConflictingAppointmentsForProfessional(any(), any(), any()))
                .thenReturn(List.of());

        Appointment conflictingAppointment = createAppointment(AppointmentType.CONSULTATION);
        when(appointmentRepository.findConflictingAppointmentsForPatient(any(), any(), any()))
                .thenReturn(List.of(conflictingAppointment));

        assertThrows(PatientNotAvailableException.class,
                () -> appointmentService.scheduleAppointment(validRequest));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void scheduleAppointment_WithTimeBlockConflict_ShouldThrowException() {

        TimeBlock timeBlock = TimeBlock.builder()
                .id(1L)
                .professionalUuid(UUID.randomUUID())
                .startDateTime(futureDateTime.minusHours(1))
                .endDateTime(futureDateTime.plusHours(2))
                .reason("Reunião")
                .type(TimeBlockType.UNAVAILABLE)
                .build();

        when(timeBlockRepository.findConflictingTimeBlocks(any(), any(), any()))
                .thenReturn(List.of(timeBlock));

        assertThrows(TimeBlockConflictException.class,
                () -> appointmentService.scheduleAppointment(validRequest));

        verify(appointmentRepository, never()).save(any());
        verify(appointmentRepository, never()).findConflictingAppointmentsForProfessional(any(), any(), any());
        verify(appointmentRepository, never()).findConflictingAppointmentsForPatient(any(), any(), any());
    }

    @Test
    void cancelAppointment_WithValidData_ShouldCancelAppointment() {

        Long appointmentId = 1L;
        UUID patientUuid = UUID.randomUUID();
        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);

        when(appointmentRepository.findByIdAndPatientUuid(appointmentId, patientUuid))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(appointment);

        AppointmentResponseDTO result = appointmentService.cancelAppointment(appointmentId, patientUuid);

        assertNotNull(result);
        assertEquals(AppointmentStatus.CANCELLED, result.status());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void cancelAppointment_WithNonExistentAppointment_ShouldThrowException() {

        Long appointmentId = 999L;
        UUID patientUuid = UUID.randomUUID();

        when(appointmentRepository.findByIdAndPatientUuid(appointmentId, patientUuid))
                .thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class,
                () -> appointmentService.cancelAppointment(appointmentId, patientUuid));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancelAppointment_WithAlreadyCancelledAppointment_ShouldThrowException() {

        Long appointmentId = 1L;
        UUID patientUuid = UUID.randomUUID();
        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findByIdAndPatientUuid(appointmentId, patientUuid))
                .thenReturn(Optional.of(appointment));

        assertThrows(AppointmentAlreadyCancelledException.class,
                () -> appointmentService.cancelAppointment(appointmentId, patientUuid));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void viewAppointments_WithDayView_ShouldReturnAppointments() {

        UUID professionalUuid = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        String viewType = "day";

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);
        List<Appointment> appointments = Arrays.asList(appointment);

        when(appointmentRepository.findByProfessionalUuidAndStartDateTimeBetween(professionalUuid, startOfDay, endOfDay))
                .thenReturn(appointments);

        // Act
        List<AppointmentViewDTO> result = appointmentService.viewAppointments(professionalUuid, date, viewType);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentRepository).findByProfessionalUuidAndStartDateTimeBetween(professionalUuid, startOfDay, endOfDay);
    }

    @Test
    void viewAppointments_WithWeekView_ShouldReturnAppointments() {

        UUID professionalUuid = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        String viewType = "week";

        LocalDateTime startOfWeek = date.atStartOfDay().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDateTime endOfWeek = startOfWeek.plusDays(6).with(LocalTime.of(23, 59, 59));

        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);
        when(appointmentRepository.findByProfessionalUuidAndStartDateTimeBetween(professionalUuid, startOfWeek, endOfWeek))
                .thenReturn(List.of(appointment));

        List<AppointmentViewDTO> result = appointmentService.viewAppointments(professionalUuid, date, viewType);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void viewAppointments_WithMonthView_ShouldReturnAppointments() {

        UUID professionalUuid = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        String viewType = "month";

        LocalDateTime startOfMonth = date.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);
        when(appointmentRepository.findByProfessionalUuidAndStartDateTimeBetween(professionalUuid, startOfMonth, endOfMonth))
                .thenReturn(List.of(appointment));

        List<AppointmentViewDTO> result = appointmentService.viewAppointments(professionalUuid, date, viewType);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void viewAppointments_WithInvalidViewType_ShouldThrowException() {

        UUID professionalUuid = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        String invalidViewType = "invalid";

        assertThrows(InvalidViewTypeException.class,
                () -> appointmentService.viewAppointments(professionalUuid, date, invalidViewType));

        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void convertToDTO_ShouldConvertAppointmentToResponseDTO() {

        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);

        AppointmentResponseDTO result = appointmentService.convertToDTO(appointment);

        assertNotNull(result);
        assertEquals(appointment.getId(), result.id());
        assertEquals(appointment.getStartDateTime(), result.startDateTime());
        assertEquals(appointment.getEndDateTime(), result.endDateTime());
        assertEquals(appointment.getProfessionalUuid(), result.professionalUuid());
        assertEquals(appointment.getPatientUuid(), result.patientUuid());
        assertEquals(appointment.getType(), result.type());
        assertEquals(appointment.getStatus(), result.status());
        assertEquals(appointment.getNotes(), result.notes());
        assertEquals(appointment.getCreatedAt(), result.createdAt());
    }

    @Test
    void convertToViewDTO_ShouldConvertAppointmentToViewDTO() {

        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);

        AppointmentViewDTO result = appointmentService.convertToViewDTO(appointment);

        assertNotNull(result);
        assertEquals(appointment.getId(), result.id());
        assertEquals(appointment.getStartDateTime(), result.startDateTime());
        assertEquals(appointment.getEndDateTime(), result.endDateTime());
        assertEquals(appointment.getProfessionalUuid(), result.professionalUuid());
        assertEquals(appointment.getPatientUuid(), result.patientUuid());
        assertEquals(appointment.getType(), result.type());
        assertEquals(appointment.getStatus(), result.status());
        assertEquals("patientName", result.patientName());
        assertEquals("professionalName", result.professionalName());
    }

    @Test
    void validateProfessionalAvailability_WithNoConflicts_ShouldNotThrowException() {

        when(appointmentRepository.findConflictingAppointmentsForProfessional(any(), any(), any()))
                .thenReturn(List.of());
        when(timeBlockRepository.findConflictingTimeBlocks(any(), any(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() ->
                appointmentService.validateProfessionalAvailability(UUID.randomUUID(), futureDateTime, futureDateTime.plusHours(1))
        );
    }

    @Test
    void validatePatientAvailability_WithNoConflicts_ShouldNotThrowException() {

        when(appointmentRepository.findConflictingAppointmentsForPatient(any(), any(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() ->
                appointmentService.validatePatientAvailability(UUID.randomUUID(), futureDateTime, futureDateTime.plusHours(1))
        );
    }

    @Test
    void scheduleAppointment_WithNullNotes_ShouldWorkCorrectly() {

        AppointmentRequestDTO requestWithNullNotes = new AppointmentRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), null, futureDateTime, futureDateTime.plusHours(1), AppointmentType.CONSULTATION
        );

        Appointment appointment = createAppointment(AppointmentType.CONSULTATION);
        appointment.setNotes(null);

        when(appointmentRepository.findConflictingAppointmentsForProfessional(any(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.findConflictingAppointmentsForPatient(any(), any(), any()))
                .thenReturn(List.of());
        when(timeBlockRepository.findConflictingTimeBlocks(any(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(appointment);

        assertDoesNotThrow(() -> appointmentService.scheduleAppointment(requestWithNullNotes));

        AppointmentResponseDTO result = appointmentService.scheduleAppointment(requestWithNullNotes);
        assertNull(result.notes());
    }

    @Test
    void scheduleAppointment_WithDifferentAppointmentTypes_ShouldWorkCorrectly() {
        AppointmentType testType = AppointmentType.PROCEDURE;

        AppointmentRequestDTO request = new AppointmentRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Notas", futureDateTime, futureDateTime.plusHours(1), testType
        );

        Appointment appointment = createAppointment(testType);

        when(appointmentRepository.findConflictingAppointmentsForProfessional(any(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.findConflictingAppointmentsForPatient(any(), any(), any()))
                .thenReturn(List.of());
        when(timeBlockRepository.findConflictingTimeBlocks(any(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(appointment);

        assertDoesNotThrow(() -> {
            AppointmentResponseDTO result = appointmentService.scheduleAppointment(request);
            assertEquals(testType, result.type());
        });
    }

    @Test
    void scheduleAppointment_ShouldPreserveAppointmentType() {

        AppointmentType[] typesToTest = {
                AppointmentType.CONSULTATION,
                AppointmentType.PROCEDURE,
                AppointmentType.URGENT,
                AppointmentType.FOLLOW_UP
        };

        for (AppointmentType expectedType : typesToTest) {
            AppointmentRequestDTO request = new AppointmentRequestDTO(
                    UUID.randomUUID(), UUID.randomUUID(), "Notas", futureDateTime, futureDateTime.plusHours(1), expectedType
            );

            Appointment appointment = createAppointment(expectedType);

            when(appointmentRepository.findConflictingAppointmentsForProfessional(any(), any(), any()))
                    .thenReturn(List.of());
            when(appointmentRepository.findConflictingAppointmentsForPatient(any(), any(), any()))
                    .thenReturn(List.of());
            when(timeBlockRepository.findConflictingTimeBlocks(any(), any(), any()))
                    .thenReturn(List.of());
            when(appointmentRepository.save(any(Appointment.class)))
                    .thenReturn(appointment);

            AppointmentResponseDTO result = appointmentService.scheduleAppointment(request);

            assertEquals(expectedType, result.type(),
                    "O tipo do appointment deve ser " + expectedType);

            reset(appointmentRepository, timeBlockRepository);
        }
    }
}