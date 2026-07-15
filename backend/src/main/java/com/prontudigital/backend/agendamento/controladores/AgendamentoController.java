package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoEnfermagemRequestDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoTratamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.PacienteAgendamentosDTO;
import com.prontudigital.backend.agendamento.dto.ReagendarRequestDTO;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;
import com.prontudigital.backend.agendamento.servicos.AgendamentoService;
import com.prontudigital.backend.agendamento.swagger.AgendamentoSwagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Criacao, cancelamento, reagendamento e visualizacao de agendamentos")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @GetMapping("/{id}")
    @BuscarPorIdSwagger
    public ResponseEntity<AgendamentoDetalhadoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    @PostMapping
    @AgendarSwagger
    public ResponseEntity<AgendamentoResponseDTO> agendar(
            @Valid @RequestBody AgendamentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agendamentoService.agendar(request));
    }

    @PatchMapping("/{id}/confirmar")
    @ConfirmarSwagger
    public ResponseEntity<Void> confirmar(@PathVariable Long id) {
        agendamentoService.confirmar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    @CancelarSwagger
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendamentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reagendar")
    @ReagendarSwagger
    public ResponseEntity<AgendamentoResponseDTO> reagendar(
            @PathVariable Long id,
            @Valid @RequestBody ReagendarRequestDTO request) {
        return ResponseEntity.ok(agendamentoService.reagendar(id, request));
    }

    @PatchMapping("/{id}/concluir")
    @ConcluirSwagger
    public ResponseEntity<Void> concluir(@PathVariable Long id) {
        agendamentoService.concluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/evolucao")
    @RegistrarEvolucaoSwagger
    public ResponseEntity<AgendamentoDetalhadoDTO> registrarEvolucao(
            @PathVariable Long id,
            @Valid @RequestBody EvolucaoTratamentoRequestDTO request) {
        return ResponseEntity.ok(agendamentoService.registrarEvolucao(id, request));
    }

    @PatchMapping("/{id}/evolucao-enfermagem")
    @RegistrarEvolucaoEnfermagemSwagger
    public ResponseEntity<AgendamentoDetalhadoDTO> registrarEvolucaoEnfermagem(
            @PathVariable Long id,
            @Valid @RequestBody EvolucaoEnfermagemRequestDTO request) {
        return ResponseEntity.ok(agendamentoService.registrarEvolucaoEnfermagem(id, request));
    }

    @GetMapping("/agenda")
    @VisualizarAgendaSwagger
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

    @GetMapping("/meus")
    @MeusAgendamentosSwagger
    public ResponseEntity<List<AgendamentoViewDTO>> obterMeus() {
        return ResponseEntity.ok(agendamentoService.obterMeusAgendamentos());
    }

    @GetMapping("/avaliacoes/{avaliacaoId}/tratamentos")
    @TratamentosPorAvaliacaoSwagger
    public ResponseEntity<List<AgendamentoViewDTO>> getTratamentosPorAvaliacao(
            @PathVariable Long avaliacaoId) {
        return ResponseEntity.ok(agendamentoService.getTratamentosPorAvaliacao(avaliacaoId));
    }

    @Operation(summary = "Listar pacientes com agendamentos nos ultimos 3 meses (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina retornada")
    @GetMapping("/pacientes")
    public ResponseEntity<Page<PacienteAgendamentosDTO>> listarPacientesComAgendamentos(
            @Parameter(description = "Filtra pelo nome do paciente")
            @RequestParam(required = false) String busca,

            @Parameter(description = "Status do agendamento, ou TODOS/omitido para nao filtrar")
            @RequestParam(required = false) String status,

            @PageableDefault(size = 10) Pageable pageable) {
        StatusAgendamento statusFiltro = (status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status))
                ? null
                : StatusAgendamento.valueOf(status);

        return ResponseEntity.ok(
                agendamentoService.listarPacientesComAgendamentos(busca, statusFiltro, pageable));
    }
}