package com.prontudigital.backend.autenticacao.controladores;

import com.prontudigital.backend.autenticacao.dto.AlterarSenhaRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AtualizarPerfilRequestDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Listar todos os usuarios (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina retornada")
    @GetMapping
    public ResponseEntity<Page<UsuarioDTO>> listarTodos(
            @PageableDefault(size = 10, sort = "nomeCompleto") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listarTodos(pageable));
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

    @Operation(summary = "Atualizar perfil do usuario (nome, e-mail, telefone)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail ja em uso")
    })
    @PatchMapping("/{id}/perfil")
    public ResponseEntity<UsuarioDTO> atualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarPerfilRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(id, dto));
    }

    @Operation(summary = "Alterar senha do usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado"),
            @ApiResponse(responseCode = "422", description = "Senha atual incorreta")
    })
    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @Valid @RequestBody AlterarSenhaRequestDTO dto) {
        usuarioService.alterarSenha(id, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deletar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario deletado"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}