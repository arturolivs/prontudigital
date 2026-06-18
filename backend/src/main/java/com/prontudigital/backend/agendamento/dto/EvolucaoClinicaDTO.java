package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.AvaliacaoPulsos;
import com.prontudigital.backend.agendamento.enums.CaracteristicaBorda;
import com.prontudigital.backend.agendamento.enums.CaracteristicaPerilesional;
import com.prontudigital.backend.agendamento.enums.ClassificacaoDor;
import com.prontudigital.backend.agendamento.enums.EvolucaoFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import com.prontudigital.backend.agendamento.enums.GrauEdema;
import com.prontudigital.backend.agendamento.enums.OdorIntensidade;
import com.prontudigital.backend.agendamento.enums.SinalEvolucao;
import com.prontudigital.backend.agendamento.enums.SinalInfeccao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Set;

@Schema(description = "Checklist de avaliação de feridas registrado no atendimento")
public record EvolucaoClinicaDTO(

        // Dados da ferida
        @Schema(description = "Localização anatômica da lesão")
        String localizacaoAnatomica,

        @Schema(description = "Etiologia da lesão")
        String etiologia,

        @Schema(description = "Tempo de evolução da lesão")
        String tempoEvolucao,

        // Mensuração
        @Schema(description = "Comprimento da lesão em cm")
        BigDecimal medidaComprimento,

        @Schema(description = "Largura da lesão em cm")
        BigDecimal medidaLargura,

        @Schema(description = "Profundidade da lesão em cm")
        BigDecimal medidaProfundidade,

        @Schema(description = "Presença de tunelização")
        Boolean tunelizacao,

        @Schema(description = "Descolamento de bordas (undermining)")
        Boolean descolamentoBordas,

        // Leito da ferida
        @Schema(description = "Percentual de epitelização")
        Integer epitelizacaoPercentual,

        @Schema(description = "Percentual de granulação")
        Integer granulacaoPercentual,

        @Schema(description = "Percentual de esfacelo/fibrina")
        Integer esfaceloPercentual,

        @Schema(description = "Percentual de necrose")
        Integer necrosePercentual,

        @Schema(description = "Tendão exposto")
        Boolean tendaoExposto,

        @Schema(description = "Músculo exposto")
        Boolean musculoExposto,

        @Schema(description = "Osso exposto")
        Boolean ossoExposto,

        // Exsudato
        @Schema(description = "Quantidade de exsudato")
        ExsudatoVolume exsudatoVolume,

        @Schema(description = "Aspecto do exsudato")
        ExsudatoCaracteristica exsudatoCaracteristica,

        @Schema(description = "Intensidade do odor")
        OdorIntensidade odorIntensidade,

        // Bordas
        @Schema(description = "Características das bordas da lesão")
        Set<CaracteristicaBorda> caracteristicasBordas,

        // Pele perilesional
        @Schema(description = "Características da pele perilesional")
        Set<CaracteristicaPerilesional> caracteristicasPerilesional,

        // Sinais de infecção
        @Schema(description = "Sinais de infecção presentes")
        Set<SinalInfeccao> sinaisInfeccao,

        // Dor
        @Schema(description = "Classificação da dor (EVA)")
        ClassificacaoDor classificacaoDor,

        // Avaliação vascular
        @Schema(description = "Grau de edema")
        GrauEdema grauEdema,

        @Schema(description = "Avaliação dos pulsos")
        AvaliacaoPulsos avaliacaoPulsos,

        // Evolução da ferida
        @Schema(description = "Evolução geral da ferida")
        EvolucaoFerida evolucaoFerida,

        @Schema(description = "Sinais de evolução observados")
        Set<SinalEvolucao> sinaisEvolucao,

        // Conduta
        @Schema(description = "Limpeza da lesão realizada")
        Boolean limpezaLesao,

        @Schema(description = "Desbridamento realizado")
        Boolean desbridamento,

        @Schema(description = "Cobertura aplicada")
        Boolean coberturaAplicada,

        @Schema(description = "Descrição da cobertura aplicada")
        String coberturaDescricao,

        @Schema(description = "Terapia adjuvante aplicada")
        Boolean terapiaAdjuvante,

        @Schema(description = "Descrição da terapia adjuvante")
        String terapiaAdjuvanteDescricao,

        @Schema(description = "Orientações fornecidas ao paciente/cuidador")
        Boolean orientacoesFornecidas,

        // Observações
        @Schema(description = "Observações gerais")
        String observacoes

) {}
