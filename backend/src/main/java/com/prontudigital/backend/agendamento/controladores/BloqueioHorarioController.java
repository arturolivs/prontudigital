package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import com.prontudigital.backend.agendamento.servicos.BloqueioHorarioService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/bloqueios-horario")
@RequiredArgsConstructor
@Tag(name = "Bloqueios de Horario (RF11)")
@SecurityRequirement(name = "bearerAuth")
public class BloqueioHorarioController {

    private final BloqueioHorarioService service;

    @Operation(summary = "Criar bloqueio de horario")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL')")
    public ResponseEntity<BloqueioHorarioDTO> criar(
            @Valid @RequestBody BloqueioHorarioDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Operation(summary = "Listar bloqueios de um profissional em um periodo")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL')")
    public ResponseEntity<List<BloqueioHorarioDTO>> listar(
            @RequestParam UUID profissionalUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(service.listarPorProfissional(profissionalUuid, inicio, fim));
    }

    @Operation(summary = "Remover bloqueio")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFISSIONAL')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}