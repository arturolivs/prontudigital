package com.prontudigital.backend.prontuario.dto;

import com.prontudigital.backend.prontuario.enums.TipoHistorico;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Item da linha do tempo do prontuario do paciente (RF18).
 * Unifica agendamentos, evolucoes clinicas, prescricoes e anexos numa
 * estrutura comum, ordenada cronologicamente pelo campo {@code data}.
 */
@Schema(description = "Evento do historico clinico do paciente")
public record HistoricoItemDTO(

        @Schema(description = "Tipo do evento", example = "PRESCRICAO")
        TipoHistorico tipo,

        @Schema(description = "Data/hora de referencia usada na ordenacao cronologica")
        LocalDateTime data,

        @Schema(description = "Titulo curto do evento", example = "Dipirona 500mg")
        String titulo,

        @Schema(description = "Detalhe secundario do evento", example = "1 comprimido a cada 6h")
        String descricao,

        @Schema(description = "UUID da entidade de origem (para navegar ate ela), quando houver")
        UUID referenciaUuid,

        @Schema(description = "UUID do agendamento associado, quando houver")
        UUID agendamentoUuid
) {}
