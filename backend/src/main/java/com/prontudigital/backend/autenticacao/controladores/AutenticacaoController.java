package com.prontudigital.backend.autenticacao.controladores;

import com.prontudigital.backend.autenticacao.dto.*;
import com.prontudigital.backend.autenticacao.servicos.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Registro, login, refresh e logout de usuarios")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @Operation(summary = "Registrar novo usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado com sucesso"),
            @ApiResponse(responseCode = "409", description = "Username ou e-mail ja existente"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    @PostMapping("/registrar")
    public ResponseEntity<UsuarioDTO> registrar(
            @Valid @RequestBody RegistrarRequestDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(autenticacaoService.registrar(request));
    }

    @Operation(summary = "Autenticar usuario e obter tokens JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> autenticar(
            @Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(autenticacaoService.autenticar(request));
    }

    @Operation(summary = "Renovar access token usando refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido ou expirado")
    })
    @PostMapping("/renovar-token")
    public ResponseEntity<RefreshTokenResponseDTO> renovarToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(autenticacaoService.renovarToken(request.refreshToken()));
    }

    @Operation(summary = "Encerrar sessao e revogar refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessao encerrada"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> encerrarSessao(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        autenticacaoService.encerrarSessao(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}