package com.prontudigital.auth_service.exceptionHandler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Resposta de erro padronizada")
public class ErrorResponse {

    @Schema(description = "Timestamp do erro", example = "2026-02-25T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Código HTTP do erro", example = "400")
    private int status;

    @Schema(description = "Nome do erro", example = "Bad Request")
    private String error;

    @Schema(description = "Mensagem de erro", example = "Dados de entrada inválidos")
    private String message;

    @Schema(description = "Caminho da requisição", example = "/api/auth/v1/register")
    private String path;

    private
    @Schema(description = "Detalhes dos erros de validação")
    Map<String, List<String>> details = new HashMap<>();

    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}