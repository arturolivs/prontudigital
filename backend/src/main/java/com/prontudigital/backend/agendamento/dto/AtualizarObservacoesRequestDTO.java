package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requisição para atualizar as observações de um agendamento")
public record AtualizarObservacoesRequestDTO(

        @Schema(description = "Texto das observações clínicas", example = "Paciente relatou melhora após sessão anterior.")
        String observacoes

) {}
