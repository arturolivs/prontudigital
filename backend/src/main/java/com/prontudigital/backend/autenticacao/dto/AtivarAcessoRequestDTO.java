package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para ativação de acesso ao sistema")
public record AtivarAcessoRequestDTO(

        @Schema(description = "Telefone cadastrado no agendamento", example = "(11) 99999-9999")
        @NotBlank
        String telefone,

        @Schema(description = "E-mail para acesso", example = "maria@email.com")
        @Email
        @NotBlank
        String email,

        @Schema(description = "Nome de usuário desejado", example = "maria.silva")
        @NotBlank
        String username,

        @Schema(description = "Senha (mínimo 8 caracteres)", example = "minhasenha123")
        @NotBlank
        @Size(min = 8, max = 30)
        String senha

) {}
