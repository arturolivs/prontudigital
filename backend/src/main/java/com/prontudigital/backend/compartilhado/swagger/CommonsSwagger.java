package com.prontudigital.backend.compartilhado.swagger;

import com.prontudigital.backend.compartilhado.dto.ErroRespostaDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class CommonsSwagger {

    private CommonsSwagger() {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "400",
            description = "Requisicao invalida",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErroRespostaDTO.class)
            )
    )
    public @interface ApiResponseBadRequest {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "401",
            description = "Nao autenticado",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErroRespostaDTO.class)
            )
    )
    public @interface ApiResponseUnauthorized {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "403",
            description = "Sem permissao para executar a operacao",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErroRespostaDTO.class)
            )
    )
    public @interface ApiResponseForbidden {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "404",
            description = "Recurso nao encontrado",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErroRespostaDTO.class)
            )
    )
    public @interface ApiResponseNotFound {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "409",
            description = "Conflito de estado ou dados",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErroRespostaDTO.class)
            )
    )
    public @interface ApiResponseConflict {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "422",
            description = "Dados invalidos ou regra de negocio violada",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErroRespostaDTO.class)
            )
    )
    public @interface ApiResponseUnprocessable {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErroRespostaDTO.class)
            )
    )
    public @interface ApiResponseInternalServerError {}
}