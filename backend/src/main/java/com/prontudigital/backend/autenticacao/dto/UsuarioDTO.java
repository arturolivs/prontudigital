package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Dados do usuario autenticado")
@Builder
public record UsuarioDTO(

        @Schema(description = "ID interno")
        Long id,

        @Schema(description = "UUID publico do usuário")
        UUID uuid,

        @Schema(description = "Nome completo", example = "Joao Silva")
        String nomeCompleto,

        @Schema(description = "Nome de usuario", example = "joao.silva")
        String username,

        @Schema(description = "E-mail", example = "joao@email.com")
        String email,

        @Schema(description = "Indica se o usuário esta ativo")
        Boolean ativo,

        @Schema(description = "Roles atribuidas ao usuário", example = "[\"USUARIO\"]")
        Set<String> roles,

        @Schema(description = "Data de criação do registro")
        Instant createdAt,

        @Schema(description = "Data da ultima atualização")
        Instant updatedAt

) {}