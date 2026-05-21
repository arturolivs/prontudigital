package com.prontudigital.backend.autenticacao.controladores;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gerenciamento de usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;

    @Operation(summary = "Retorna dados do usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados retornados"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado")
    })
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> getUsuarioAtual() {
        return ResponseEntity.ok(usuarioContexto.getUsuarioAtual());
    }

    @Operation(summary = "Listar todos os usuarios")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(summary = "Buscar usuario por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @Operation(summary = "Buscar usuario por UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<UsuarioDTO> buscarPorUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(usuarioService.buscarPorUuid(uuid));
    }

    @Operation(summary = "Atualizar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail ja em uso")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    @Operation(summary = "Deletar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario deletado"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}