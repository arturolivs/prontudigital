package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Confirmação do código de recuperação e definição de nova senha")
public record RecuperarSenhaConfirmarRequestDTO(

        @Schema(description = "Telefone cadastrado do usuário (com DDD)", example = "11999999999")
        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @Schema(description = "Código de 6 dígitos recebido via WhatsApp")
        @NotBlank(message = "O código é obrigatório")
        @Pattern(regexp = "\\d{6}", message = "O código deve conter 6 dígitos")
        String codigo,

        @Schema(description = "Nova senha desejada")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres")
        String novaSenha

) {}
