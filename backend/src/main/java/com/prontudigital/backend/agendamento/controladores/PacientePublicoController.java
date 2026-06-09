package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.ProfissionalPublicoDTO;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
                .map(u -> new ProfissionalPublicoDTO(u.uuid(), u.nomeCompleto()))
                .toList();
        return ResponseEntity.ok(profissionais);
    }
}
