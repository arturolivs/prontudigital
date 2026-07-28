package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisicao para criacao/atualizacao de procedimento")
public record ProcedimentoRequestDTO(

        @Schema(description = "Nome do procedimento", example = "Podiatria")
        @NotBlank @Size(max = 100) String nome,

        @Schema(description = "Descricao do procedimento")
        String descricao,

        @Schema(description = "Duracao padrao em minutos", example = "60")
        @Positive Integer duracaoPadraoMinutos,

        @Schema(description = "Indica se o procedimento esta disponivel para agendamento (padrao: true)")
        Boolean ativo

) {}
