package com.prontudigital.backend.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Schema(description = "Relatorio de ocupacao da clinica (RF20)")
@Builder
public record RelatorioOcupacaoDTO(

        LocalDate inicio,

        LocalDate fim,

        @Schema(description = "Nome do profissional filtrado; nulo quando cobre a clinica inteira")
        String nomeProfissional,

        @Schema(description = "Total de agendamentos no periodo, qualquer status")
        int total,

        int realizados,

        int cancelados,

        int naoCompareceram,

        int remarcados,

        @Schema(description = "Ainda em aberto: AGENDADO ou CONFIRMADO")
        int emAberto,

        @Schema(description = "Percentual de comparecimento sobre os atendimentos que "
                + "chegaram a acontecer (realizados + faltas). Nulo quando nao houve nenhum.")
        Double taxaComparecimento,

        @Schema(description = "Percentual de cancelamento sobre o total. Nulo quando o "
                + "periodo nao teve agendamentos.")
        Double taxaCancelamento,

        @Schema(description = "Percentual de faltas sobre os atendimentos que chegaram a "
                + "acontecer. Nulo quando nao houve nenhum.")
        Double taxaAbsenteismo,

        @Schema(description = "Horas efetivamente ocupadas na agenda (exclui cancelados "
                + "e remarcados)")
        double horasAgendadas,

        @Schema(description = "Horas de expediente no periodo, a partir dos horarios de "
                + "trabalho (RF05). Nulo quando o relatorio nao filtra um profissional "
                + "ou quando ele nao tem expediente cadastrado.")
        Double horasDisponiveis,

        @Schema(description = "horasAgendadas / horasDisponiveis. Nulo pelos mesmos "
                + "motivos de horasDisponiveis.")
        Double taxaOcupacao

) {}
