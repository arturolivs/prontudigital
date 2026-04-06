package com.prontudigital.auth_service.config.doc;

import com.prontudigital.auth_service.dto.JwtResponseDTO;
import com.prontudigital.auth_service.dto.RefreshTokenResponseDTO;
import com.prontudigital.auth_service.dto.UserResponseDTO;
import com.prontudigital.auth_service.exceptionHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ApiResponseDocs {

    private ApiResponseDocs() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Dados de entrada inválidos",
                        "path": "/api/auth/v1/register",
                        "details": {
                            "password": ["Senha deve ter entre 8 e 30 caracteres"],
                            "email": ["Email inválido"]
                        }
                    }
                """))),
            @ApiResponse(responseCode = "401", description = "Não autorizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Bad credentials",
                        "path": "/api/auth/v1/signin"
                    }
                """))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Usuário não encontrado",
                        "path": "/api/auth/v1/user/123"
                    }
                """))),
            @ApiResponse(responseCode = "409", description = "Conflito",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 409,
                        "error": "Conflict",
                        "message": "Username already exists",
                        "path": "/api/auth/v1/register"
                    }
                """))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-25T10:30:00",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "An unexpected error occurred",
                        "path": "/api/auth/v1/signin"
                    }
                """)))
    })
    public @interface StandardApiResponses {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro realizado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "fullName": "João Silva",
                        "email": "joao@email.com",
                        "username": "joaosilva",
                        "isActive": true
                    }
                """)))
    })
    public @interface RegisterSuccessResponse {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponseDTO.class),
                            examples = @ExampleObject(value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 900,
                        "username": "joaosilva",
                        "roles": ["USER"]
                    }
                """)))
    })
    public @interface LoginSuccessResponse {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RefreshTokenResponseDTO.class),
                            examples = @ExampleObject(value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 900
                    }
                """)))
    })
    public @interface RefreshTokenSuccessResponse {
    }
}