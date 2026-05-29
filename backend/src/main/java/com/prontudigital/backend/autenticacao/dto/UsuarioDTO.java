package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Dados do usuário")
@Builder
public record UsuarioDTO(

        @Schema(description = "ID interno")
        Long id,

        @Schema(description = "UUID publico do usuário")
        UUID uuid,

        @Schema(description = "Nome completo", example = "Joao Silva")
        String nomeCompleto,

        @Schema(description = "Nome de usuÁrio", example = "joaosilva")
        String username,

        @Schema(description = "E-mail", example = "joao@email.com")
        String email,

        @Schema(description = "Telefone", example = "(11) 99999-9999")
        String telefone,

        @Schema(description = "Indica se o usuário esta ativo")
        Boolean ativo,

        @Schema(description = "Indica se o usuário possui acesso ao sistema configurado")
        Boolean acessoAtivado,

        @Schema(description = "Perfis atribuídos ao usuário", example = "[\"USUARIO\"]")
        Set<String> perfis,

        @Schema(description = "Data de criação do registro")
        Instant criadoEm,

        @Schema(description = "Data da ultima atualização")
        Instant atualizadoEm

) {}