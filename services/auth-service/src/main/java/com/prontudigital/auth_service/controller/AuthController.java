package com.prontudigital.auth_service.controller;

import com.prontudigital.auth_service.config.doc.ApiResponseDocs;
import com.prontudigital.auth_service.dto.*;
import com.prontudigital.auth_service.exception.InvalidTokenException;
import com.prontudigital.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação e gerenciamento de tokens")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Registrar novo usuário",
            description = "Cria uma nova conta de usuário no sistema. Por padrão, atribui a role 'USER'."
    )
    @ApiResponseDocs.RegisterSuccessResponse
    @ApiResponseDocs.StandardApiResponses
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/signin")
    @Operation(
            summary = "Autenticar usuário",
            description = "Realiza login e retorna tokens de acesso JWT"
    )
    @ApiResponseDocs.LoginSuccessResponse
    @ApiResponseDocs.StandardApiResponses
    public ResponseEntity<JwtResponseDTO> authenticate(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Renovar token de acesso",
            description = "Gera um novo access token utilizando um refresh token válido"
    )
    @ApiResponseDocs.RefreshTokenSuccessResponse
    @ApiResponse(responseCode = "401", description = "Refresh token inválido ou revogado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = com.prontudigital.auth_service.exceptionHandler.ErrorResponse.class),
                    examples = @ExampleObject(value = """
                {
                    "timestamp": "2026-02-25T10:30:00",
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "Refresh token inválido",
                    "path": "/api/auth/v1/refresh-token"
                }
            """)))
    @ApiResponseDocs.StandardApiResponses
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        try {
            RefreshTokenResponseDTO response = authService.refreshToken(request.refreshToken());
            return ResponseEntity.ok(response);
        } catch (InvalidTokenException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Invalida o refresh token do usuário"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token inválido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.prontudigital.auth_service.exceptionHandler.ErrorResponse.class)))
    })
    @ApiResponseDocs.StandardApiResponses
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}