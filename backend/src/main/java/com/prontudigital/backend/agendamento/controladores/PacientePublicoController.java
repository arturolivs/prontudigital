package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.ProfissionalPublicoDTO;
import com.prontudigital.backend.autenticacao.dto.AvatarDownloadDTO;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Público")
public class PacientePublicoController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar profissionais disponíveis para agendamento")
    @SecurityRequirements
    @GetMapping("/profissionais")
    public ResponseEntity<List<ProfissionalPublicoDTO>> listarProfissionais() {
        List<ProfissionalPublicoDTO> profissionais = usuarioService.listarTodos()
                .stream()
                .filter(u -> u.perfis() != null && u.perfis().contains("PROFISSIONAL"))
                .filter(u -> Boolean.TRUE.equals(u.ativo()))
                .map(u -> new ProfissionalPublicoDTO(
                        u.uuid(), u.nomeCompleto(), Boolean.TRUE.equals(u.temAvatar())))
                .toList();
        return ResponseEntity.ok(profissionais);
    }

    @Operation(summary = "Obter a foto de um profissional",
            description = "Binario inline para uso em <img> na tela publica de "
                    + "agendamento. Responde 422 para uuid que nao seja de um "
                    + "profissional ativo com foto cadastrada.")
    @SecurityRequirements
    @GetMapping("/profissionais/{uuid}/avatar")
    public ResponseEntity<Resource> baixarAvatarProfissional(@PathVariable UUID uuid) {
        AvatarDownloadDTO avatar = usuarioService.baixarAvatarProfissional(uuid);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avatar\"")
                .body(avatar.recurso());
    }
}
