package com.prontudigital.backend.prontuario.controladores;

import com.prontudigital.backend.prontuario.dto.PrescricaoRequestDTO;
import com.prontudigital.backend.prontuario.dto.PrescricaoResponseDTO;
import com.prontudigital.backend.prontuario.servicos.PrescricaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prontuario/pacientes/{pacienteUuid}/prescricoes")
@RequiredArgsConstructor
@Tag(name = "Prontuario - Prescricoes",
        description = "Prescricao de medicamentos e cuidados de enfermagem (RF16)")
@SecurityRequirement(name = "bearerAuth")
public class PrescricaoController {

    private final PrescricaoService prescricaoService;

    @GetMapping
    @Operation(summary = "Listar prescricoes do paciente",
            description = "ADMIN acessa qualquer paciente; PROFISSIONAL apenas pacientes com quem "
                    + "possui agendamento; PACIENTE apenas as proprias prescricoes.")
    @ApiResponse(responseCode = "200", description = "Lista de prescricoes (ordenadas da mais recente)")
    public ResponseEntity<List<PrescricaoResponseDTO>> listar(@PathVariable UUID pacienteUuid) {
        return ResponseEntity.ok(prescricaoService.listarPorPaciente(pacienteUuid));
    }

    @PostMapping
    @Operation(summary = "Registrar prescricao",
            description = "Cria uma prescricao. Restrito a ADMIN e ao PROFISSIONAL vinculado ao paciente.")
    @ApiResponse(responseCode = "201", description = "Prescricao registrada")
    public ResponseEntity<PrescricaoResponseDTO> registrar(
            @PathVariable UUID pacienteUuid,
            @Valid @RequestBody PrescricaoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescricaoService.registrar(pacienteUuid, request));
    }

    @PutMapping("/{prescricaoUuid}")
    @Operation(summary = "Atualizar prescricao",
            description = "Atualiza uma prescricao existente. Restrito a ADMIN e ao PROFISSIONAL vinculado.")
    @ApiResponse(responseCode = "200", description = "Prescricao atualizada")
    @ApiResponse(responseCode = "404", description = "Prescricao nao encontrada para o paciente")
    public ResponseEntity<PrescricaoResponseDTO> atualizar(
            @PathVariable UUID pacienteUuid,
            @PathVariable UUID prescricaoUuid,
            @Valid @RequestBody PrescricaoRequestDTO request) {
        return ResponseEntity.ok(prescricaoService.atualizar(pacienteUuid, prescricaoUuid, request));
    }

    @DeleteMapping("/{prescricaoUuid}")
    @Operation(summary = "Excluir prescricao",
            description = "Remove uma prescricao. Restrito a ADMIN e ao PROFISSIONAL vinculado.")
    @ApiResponse(responseCode = "204", description = "Prescricao excluida")
    @ApiResponse(responseCode = "404", description = "Prescricao nao encontrada para o paciente")
    public ResponseEntity<Void> excluir(
            @PathVariable UUID pacienteUuid,
            @PathVariable UUID prescricaoUuid) {
        prescricaoService.excluir(pacienteUuid, prescricaoUuid);
        return ResponseEntity.noContent().build();
    }
}
