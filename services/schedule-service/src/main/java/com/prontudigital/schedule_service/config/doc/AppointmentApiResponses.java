package com.prontudigital.schedule_service.config.doc;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class AppointmentApiResponses {

    private AppointmentApiResponses() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.prontudigital.schedule_service.exceptionHandler.ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Dados de entrada inválidos",
                        "path": "/api/appointments/schedule",
                        "details": {
                            "startDateTime": ["não pode ser nulo"]
                        }
                    }
                """))),
            @ApiResponse(responseCode = "401", description = "Não autorizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.prontudigital.schedule_service.exceptionHandler.ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token inválido ou expirado",
                        "path": "/api/appointments/schedule"
                    }
                """))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.prontudigital.schedule_service.exceptionHandler.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.prontudigital.schedule_service.exceptionHandler.ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Consulta não encontrada",
                        "path": "/api/appointments/1/cancel"
                    }
                """))),
            @ApiResponse(responseCode = "409", description = "Conflito (horário indisponível)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.prontudigital.schedule_service.exceptionHandler.ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 409,
                        "error": "Conflict",
                        "message": "Horário já está ocupado",
                        "path": "/api/appointments/schedule"
                    }
                """))),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.prontudigital.schedule_service.exceptionHandler.ErrorResponse.class)))
    })
    public @interface StandardAppointmentResponses {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Consulta agendada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class),
                            examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "startDateTime": "2026-02-26T14:00:00",
                        "endDateTime": "2026-02-26T14:30:00",
                        "professionalUuid": "123e4567-e89b-12d3-a456-426614174000",
                        "patientUuid": "123e4567-e89b-12d3-a456-426614174001",
                        "type": "CONSULTA",
                        "status": "SCHEDULED",
                        "notes": "Paciente com sintomas de gripe",
                        "createdAt": "2026-02-25T10:00:00"
                    }
                """)))
    })
    public @interface ScheduleSuccessResponse {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta cancelada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class),
                            examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "startDateTime": "2026-02-26T14:00:00",
                        "endDateTime": "2026-02-26T14:30:00",
                        "professionalUuid": "123e4567-e89b-12d3-a456-426614174000",
                        "patientUuid": "123e4567-e89b-12d3-a456-426614174001",
                        "type": "CONSULTA",
                        "status": "CANCELED",
                        "notes": "Paciente com sintomas de gripe",
                        "createdAt": "2026-02-25T10:00:00"
                    }
                """)))
    })
    public @interface CancelSuccessResponse {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de consultas recuperada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentViewDTO.class),
                            examples = @ExampleObject(value = """
                    [
                        {
                            "id": 1,
                            "startDateTime": "2026-02-26T14:00:00",
                            "endDateTime": "2026-02-26T14:30:00",
                            "professionalUuid": "123e4567-e89b-12d3-a456-426614174000",
                            "patientUuid": "123e4567-e89b-12d3-a456-426614174001",
                            "type": "CONSULTA",
                            "status": "SCHEDULED",
                            "patientName": "João Silva",
                            "professionalName": "Dra. Maria Oliveira"
                        }
                    ]
                """)))
    })
    public @interface ViewSuccessResponse {
    }
}
