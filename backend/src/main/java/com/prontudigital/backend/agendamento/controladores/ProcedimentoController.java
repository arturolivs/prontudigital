package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.ProcedimentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.ProcedimentoResponseDTO;
import com.prontudigital.backend.agendamento.servicos.ProcedimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimentos")
@RequiredArgsConstructor
@Tag(name = "Procedimentos",
        description = "Catalogo de servicos/procedimentos da clinica (RF06)")
@SecurityRequirement(name = "bearerAuth")
public class ProcedimentoController {

    private final ProcedimentoService procedimentoService;

    @GetMapping
    @Operation(summary = "Listar procedimentos",
            description = "Lista os procedimentos ativos (ordenados por nome). "
                    + "Use incluirInativos=true para listar tambem os desativados.")
    @ApiResponse(responseCode = "200", description = "Lista de procedimentos")
    public ResponseEntity<List<ProcedimentoResponseDTO>> listar(
            @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return ResponseEntity.ok(procedimentoService.listar(incluirInativos));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar procedimento por id")
    @ApiResponse(responseCode = "200", description = "Procedimento encontrado")
    @ApiResponse(responseCode = "404", description = "Procedimento nao encontrado")
    public ResponseEntity<ProcedimentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(procedimentoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar procedimento", description = "Restrito a ADMIN.")
    @ApiResponse(responseCode = "201", description = "Procedimento criado")
    @ApiResponse(responseCode = "409", description = "Ja existe procedimento com o mesmo nome")
    public ResponseEntity<ProcedimentoResponseDTO> criar(
            @Valid @RequestBody ProcedimentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procedimentoService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar procedimento",
            description = "Restrito a ADMIN. Permite tambem ativar/desativar via campo 'ativo'.")
    @ApiResponse(responseCode = "200", description = "Procedimento atualizado")
    @ApiResponse(responseCode = "404", description = "Procedimento nao encontrado")
    public ResponseEntity<ProcedimentoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProcedimentoRequestDTO request) {
        return ResponseEntity.ok(procedimentoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir procedimento",
            description = "Restrito a ADMIN. Procedimentos ja vinculados a agendamentos nao "
                    + "podem ser excluidos; nesse caso, desative-os pela atualizacao.")
    @ApiResponse(responseCode = "204", description = "Procedimento excluido")
    @ApiResponse(responseCode = "404", description = "Procedimento nao encontrado")
    @ApiResponse(responseCode = "409", description = "Procedimento vinculado a agendamentos")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        procedimentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
