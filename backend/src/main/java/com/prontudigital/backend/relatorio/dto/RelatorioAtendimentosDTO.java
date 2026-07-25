package com.prontudigital.backend.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "Relatorio de atendimentos por periodo/profissional (RF19)")
@Builder
public record RelatorioAtendimentosDTO(

        LocalDate inicio,

        LocalDate fim,

        @Schema(description = "Nome do profissional filtrado; nulo quando o relatorio "
                + "cobre a clinica inteira")
        String nomeProfissional,

        @Schema(description = "Total de atendimentos no recorte")
        int total,

        @Schema(description = "Quantidade por status, incluindo apenas os status presentes")
        Map<String, Long> totalPorStatus,

        @Schema(description = "Quantidade por tipo de agendamento (AVALIACAO/TRATAMENTO)")
        Map<String, Long> totalPorTipo,

        List<RelatorioAtendimentoItemDTO> itens

) {}
