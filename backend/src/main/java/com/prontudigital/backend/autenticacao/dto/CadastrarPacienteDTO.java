package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para cadastro rápido de paciente")
public record CadastrarPacienteDTO(

        @Schema(description = "Nome completo", example = "Maria da Silva")
        @NotBlank
        String nomeCompleto,

        @Schema(description = "Telefone", example = "(11) 99999-9999")
        @NotBlank
        String telefone

) {}
