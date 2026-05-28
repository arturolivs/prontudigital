package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.AtualizarObservacoesRequestDTO;
import com.prontudigital.backend.agendamento.dto.ReagendarRequestDTO;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;
import com.prontudigital.backend.agendamento.servicos.AgendamentoService;
import com.prontudigital.backend.agendamento.swagger.AgendamentoSwagger.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PatchMapping("/{id}/observacoes")
    @AtualizarObservacoesSwagger
    public ResponseEntity<AgendamentoDetalhadoDTO> atualizarObservacoes(
            @PathVariable Long id,
            @RequestBody AtualizarObservacoesRequestDTO request) {
        return ResponseEntity.ok(agendamentoService.atualizarObservacoes(id, request.observacoes()));
    }

    @PostMapping
    @AgendarSwagger
    public ResponseEntity<AgendamentoResponseDTO> agendar(
            @Valid @RequestBody AgendamentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agendamentoService.agendar(request));
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
}