package com.prontudigital.backend.prontuario.controladores;

import com.prontudigital.backend.prontuario.dto.AnamneseRequestDTO;
import com.prontudigital.backend.prontuario.dto.AnamneseResponseDTO;
import com.prontudigital.backend.prontuario.servicos.AnamneseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/prontuario/pacientes/{pacienteUuid}/anamnese")
@RequiredArgsConstructor
@Tag(name = "Prontuario - Anamnese", description = "Registro de anamnese do paciente (RF13)")
@SecurityRequirement(name = "bearerAuth")
public class AnamneseController {

    private final AnamneseService anamneseService;

    @GetMapping
    @Operation(summary = "Buscar anamnese do paciente",
            description = "ADMIN acessa qualquer paciente; PROFISSIONAL apenas pacientes com quem "
                    + "possui agendamento; PACIENTE apenas a propria anamnese.")
    @ApiResponse(responseCode = "200", description = "Anamnese encontrada")
    @ApiResponse(responseCode = "404", description = "Paciente ainda nao possui anamnese")
    public ResponseEntity<AnamneseResponseDTO> buscar(@PathVariable UUID pacienteUuid) {
        return ResponseEntity.ok(anamneseService.buscarPorPaciente(pacienteUuid));
    }

    @PostMapping
    @Operation(summary = "Registrar anamnese do paciente",
            description = "Cria a anamnese. Restrito a ADMIN e ao PROFISSIONAL vinculado ao paciente.")
    @ApiResponse(responseCode = "201", description = "Anamnese registrada")
    @ApiResponse(responseCode = "409", description = "Paciente ja possui anamnese")
    public ResponseEntity<AnamneseResponseDTO> registrar(
            @PathVariable UUID pacienteUuid,
            @Valid @RequestBody AnamneseRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(anamneseService.registrar(pacienteUuid, request));
    }

    @PutMapping
    @Operation(summary = "Atualizar anamnese do paciente",
            description = "Atualiza a anamnese existente. Restrito a ADMIN e ao PROFISSIONAL vinculado.")
    @ApiResponse(responseCode = "200", description = "Anamnese atualizada")
    @ApiResponse(responseCode = "404", description = "Paciente ainda nao possui anamnese")
    public ResponseEntity<AnamneseResponseDTO> atualizar(
            @PathVariable UUID pacienteUuid,
            @Valid @RequestBody AnamneseRequestDTO request) {
        return ResponseEntity.ok(anamneseService.atualizar(pacienteUuid, request));
    }
}
