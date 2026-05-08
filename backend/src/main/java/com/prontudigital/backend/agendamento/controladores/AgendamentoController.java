package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.*;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;
import com.prontudigital.backend.agendamento.servicos.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Agendamentos")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @Operation(summary = "Criar novo agendamento (RF07)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL', 'PACIENTE')")
    public ResponseEntity<AgendamentoResponseDTO> agendar(
            @Valid @RequestBody AgendamentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agendamentoService.agendar(request));
    }

    @Operation(summary = "Cancelar agendamento (RF10)")
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL', 'PACIENTE')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendamentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reagendar (RF10)")
    @PatchMapping("/{id}/reagendar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL', 'PACIENTE')")
    public ResponseEntity<AgendamentoResponseDTO> reagendar(
            @PathVariable Long id,
            @Valid @RequestBody ReagendarRequestDTO request) {
        return ResponseEntity.ok(agendamentoService.reagendar(id, request));
    }

    @Operation(summary = "Concluir agendamento")
    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL')")
    public ResponseEntity<Void> concluir(@PathVariable Long id) {
        agendamentoService.concluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Visualizar agenda dia/semana/mes (RF08)")
    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL')")
    public ResponseEntity<List<AgendamentoViewDTO>> visualizarAgenda(
            @Parameter(description = "Data de referencia", example = "2026-05-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,

            @Parameter(description = "DIA, SEMANA ou MES")
            @RequestParam(defaultValue = "DIA") TipoVisualizacaoAgenda tipo,

            @Parameter(description = "Obrigatorio quando ADMIN; ignorado para PROFISSIONAL")
            @RequestParam(required = false) UUID profissionalUuid) {
        return ResponseEntity.ok(
                agendamentoService.visualizarAgenda(data, tipo, profissionalUuid));
    }

    @Operation(summary = "Listar tratamentos da avaliacao")
    @GetMapping("/avaliacoes/{avaliacaoId}/tratamentos")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL', 'PACIENTE')")
    public ResponseEntity<List<AgendamentoViewDTO>> getTratamentosPorAvaliacao(
            @PathVariable Long avaliacaoId) {
        return ResponseEntity.ok(agendamentoService.getTratamentosPorAvaliacao(avaliacaoId));
    }
}