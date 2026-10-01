package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para alteração de senha")
public record AlterarSenhaRequestDTO(

        @Schema(description = "Senha atual do usuário")
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

        @Schema(description = "Nova senha desejada")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres")
        String novaSenha

) {}
