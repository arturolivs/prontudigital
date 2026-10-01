package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.HorarioTrabalhoDTO;
import com.prontudigital.backend.agendamento.servicos.HorarioTrabalhoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/horarios-trabalho")
@RequiredArgsConstructor
@Tag(name = "Horarios de Trabalho (RF05)")
@SecurityRequirement(name = "bearerAuth")
public class HorarioTrabalhoController {

    private final HorarioTrabalhoService service;

    @Operation(summary = "Criar janela de atendimento do profissional")
    @ApiResponse(responseCode = "201", description = "Janela criada")
    @ApiResponse(responseCode = "403", description = "Sem autorizacao para a agenda informada")
    @ApiResponse(responseCode = "422", description = "Janela invalida ou sobreposta")
    @PostMapping
    public ResponseEntity<HorarioTrabalhoDTO> criar(
            @Valid @RequestBody HorarioTrabalhoDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Operation(summary = "Listar janelas de atendimento de um profissional")
    @GetMapping
    public ResponseEntity<List<HorarioTrabalhoDTO>> listar(
            @RequestParam UUID profissionalUuid) {
        return ResponseEntity.ok(service.listarPorProfissional(profissionalUuid));
    }

    @Operation(summary = "Remover janela de atendimento")
    @ApiResponse(responseCode = "204", description = "Janela removida")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar janelas de um profissional (público, sem autenticação)",
            description = "Usado pela tela publica de agendamento para oferecer so os "
                    + "horarios em que o profissional atende.")
    @SecurityRequirements
    @GetMapping("/public")
    public ResponseEntity<List<HorarioTrabalhoDTO>> listarPublico(
            @RequestParam UUID profissionalUuid) {
        return ResponseEntity.ok(service.listarPorProfissional(profissionalUuid));
    }
}
