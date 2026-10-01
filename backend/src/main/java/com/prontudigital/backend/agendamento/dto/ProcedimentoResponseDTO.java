package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados do procedimento")
public record ProcedimentoResponseDTO(

        Long id,

        @Schema(description = "Codigo legado do enum TipoProcedimento (apenas registros migrados)")
        String codigo,

        String nome,
        String descricao,
        Integer duracaoPadraoMinutos,
        Boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm

) {}
