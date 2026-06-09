package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualização do perfil do usuário")
public record AtualizarPerfilRequestDTO(

        @Schema(description = "Nome completo", example = "João Silva")
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nomeCompleto,

        @Schema(description = "E-mail", example = "joao@email.com")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres")
        String email,

        @Schema(description = "Telefone", example = "(11) 9 9999-9999")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone

) {}
