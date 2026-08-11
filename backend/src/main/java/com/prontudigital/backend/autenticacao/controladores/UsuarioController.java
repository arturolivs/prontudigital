package com.prontudigital.backend.autenticacao.controladores;

import com.prontudigital.backend.autenticacao.dto.AlterarSenhaRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AtualizarPerfilRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AvatarDownloadDTO;
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
import org.springframework.core.io.Resource;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // ── Avatar do usuario autenticado ────────────────────────────────
    //
    // Sem id no caminho de proposito: o avatar e editado em "Meu perfil" e
    // vale sempre para quem esta na sessao. Isso dispensa checagem de posse,
    // que o PATCH /{id}/perfil deixa a cargo de quem chama.

    @Operation(summary = "Enviar avatar do usuario autenticado",
            description = "Imagem JPG, PNG ou WEBP de ate 2 MB. Substitui a anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar atualizado"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "422",
                    description = "Arquivo vazio, tipo nao permitido ou acima do limite")
    })
    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioDTO> enviarAvatar(
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(usuarioService.enviarAvatar(arquivo));
    }

    @Operation(summary = "Obter o avatar do usuario autenticado",
            description = "Retorna o binario inline, para uso em <img>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Binario do avatar"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "422", description = "Nenhum avatar cadastrado")
    })
    @GetMapping("/me/avatar")
    public ResponseEntity<Resource> baixarAvatar() {
        AvatarDownloadDTO avatar = usuarioService.baixarAvatar();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avatar\"")
                .body(avatar.recurso());
    }

    @Operation(summary = "Remover o avatar do usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar removido"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado")
    })
    @DeleteMapping("/me/avatar")
    public ResponseEntity<UsuarioDTO> removerAvatar() {
        return ResponseEntity.ok(usuarioService.removerAvatar());
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