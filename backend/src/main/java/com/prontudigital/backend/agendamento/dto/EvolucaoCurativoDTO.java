package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.AvaliacaoEvolucao;
import com.prontudigital.backend.agendamento.enums.BordasFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoTime;
import com.prontudigital.backend.agendamento.enums.InfeccaoInflamacaoCurativo;
import com.prontudigital.backend.agendamento.enums.TecidoLeito;
import com.prontudigital.backend.agendamento.enums.TipoDesbridamento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Ficha de Evolucao Diaria - Curativos registrada no tratamento")
@Builder
public record EvolucaoCurativoDTO(

        // 1. Avaliacao diaria
        BigDecimal comprimento,
        BigDecimal largura,
        BigDecimal profundidade,
        BigDecimal areaAproximada,
        TecidoLeito tecido,
        InfeccaoInflamacaoCurativo infeccaoInflamacao,
        ExsudatoTime exsudato,
        BordasFerida bordas,
        Boolean odorPresente,
        Integer dorEscala,
        String pelePerilesional,

        // 2. Intervencoes
        String limpezaIrrigacao,
        TipoDesbridamento desbridamento,
        String desbridamentoObs,
        String coberturaPrimaria,
        String orientacoesPaciente,

        // 3. Avaliacao da evolucao
        AvaliacaoEvolucao evolucao,
        String observacoes,

        // 4. Plano / acoes futuras
        String planoManterConduta,
        String planoAlterarCobertura,
        String planoSolicitarExames,
        String planoEncaminhamento,
        String retornoPrevisto,

        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm

) {}
