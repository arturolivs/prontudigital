package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.FilaEsperaDTO;
import com.prontudigital.backend.agendamento.dto.FilaEsperaRequestDTO;
import com.prontudigital.backend.agendamento.servicos.FilaEsperaService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/fila-espera")
@RequiredArgsConstructor
@Tag(name = "Fila de Espera (RF12)")
@SecurityRequirement(name = "bearerAuth")
public class FilaEsperaController {

    private final FilaEsperaService service;

    @Operation(summary = "Entrar na fila de espera")
    @PostMapping
    public ResponseEntity<FilaEsperaDTO> entrar(
            @Valid @RequestBody FilaEsperaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.entrar(request));
    }

    @Operation(summary = "Sair da fila")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sair(@PathVariable Long id) {
        service.sair(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar fila ativa de um profissional")
    @GetMapping
    public ResponseEntity<List<FilaEsperaDTO>> listar(
            @RequestParam UUID profissionalUuid) {
        return ResponseEntity.ok(service.listarFilaDoProfissional(profissionalUuid));
    }

    @Operation(summary = "Marcar entrada da fila como notificada")
    @PatchMapping("/{id}/notificar")
    public ResponseEntity<Void> marcarNotificado(@PathVariable Long id) {
        service.marcarComoNotificado(id);
        return ResponseEntity.noContent().build();
    }
}