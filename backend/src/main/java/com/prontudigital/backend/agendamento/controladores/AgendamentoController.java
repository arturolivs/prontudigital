package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.servicos.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Criacao, cancelamento e visualizacao de agendamentos")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @Operation(summary = "Criar novo agendamento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "Conflito de horario"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissao")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL', 'PACIENTE')")
    public ResponseEntity<AgendamentoResponseDTO> agendar(
            @Valid @RequestBody AgendamentoRequestDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(agendamentoService.agendar(request));
    }

    @Operation(summary = "Cancelar agendamento")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cancelado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Agendamento ja cancelado ou concluido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao para cancelar")
    })
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL', 'PACIENTE')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendamentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Concluir agendamento")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Concluido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Agendamento ja concluido ou cancelado"),
            @ApiResponse(responseCode = "403", description = "Sem permissao para concluir")
    })
    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL')")
    public ResponseEntity<Void> concluir(@PathVariable Long id) {
        agendamentoService.concluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Visualizar agenda",
            description = "Retorna agendamentos do profissional autenticado. " +
                    "tipoVisualizacao aceita: 'dia', 'semana' ou 'mes'"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda retornada"),
            @ApiResponse(responseCode = "400", description = "Tipo de visualizacao invalido"),
            @ApiResponse(responseCode = "403", description = "Paciente nao pode visualizar agenda")
    })
    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL')")
    public ResponseEntity<List<AgendamentoViewDTO>> visualizarAgenda(
            @Parameter(description = "Data de referencia (formato: yyyy-MM-dd)", example = "2026-05-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,

            @Parameter(description = "Tipo: 'dia', 'semana' ou 'mes'", example = "semana")
            @RequestParam(defaultValue = "dia") String tipoVisualizacao) {
        return ResponseEntity.ok(agendamentoService.visualizarAgenda(data, tipoVisualizacao));
    }

    @Operation(summary = "Listar tratamentos vinculados a uma avaliacao")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tratamentos retornados"),
            @ApiResponse(responseCode = "404", description = "Avaliacao nao encontrada"),
            @ApiResponse(responseCode = "403", description = "Sem permissao para visualizar")
    })
    @GetMapping("/avaliacoes/{avaliacaoId}/tratamentos")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL', 'PACIENTE')")
    public ResponseEntity<List<AgendamentoViewDTO>> getTratamentosPorAvaliacao(
            @PathVariable Long avaliacaoId) {
        return ResponseEntity.ok(agendamentoService.getTratamentosPorAvaliacao(avaliacaoId));
    }
}