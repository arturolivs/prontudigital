package com.prontudigital.backend.relatorio.dto;

import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Linha do relatorio de atendimentos (RF19)")
@Builder
public record RelatorioAtendimentoItemDTO(

        Long agendamentoId,

        LocalDateTime inicioEm,

        LocalDateTime fimEm,

        @Schema(description = "Duracao do atendimento em minutos")
        long duracaoMinutos,

        UUID pacienteUuid,

        String nomePaciente,

        UUID profissionalUuid,

        String nomeProfissional,

        TipoAgendamento tipo,

        String procedimentoNome,

        LocalAtendimento localAtendimento,

        StatusAgendamento status,

        @Schema(description = "Quando o atendimento foi concluido; nulo se nao realizado")
        LocalDateTime concluidoEm

) {}
