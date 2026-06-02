package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Evolução clínica de enfermagem registrada no atendimento")
public record EvolucaoClinicaDTO(

        @Schema(description = "Localização anatômica da lesão")
        String localizacaoAnatomica,

        @Schema(description = "Tipo de lesão (LPP, úlcera venosa, pé diabético etc.)")
        String tipoLesao,

        @Schema(description = "Comprimento da lesão em cm")
        BigDecimal medidaComprimento,

        @Schema(description = "Largura da lesão em cm")
        BigDecimal medidaLargura,

        @Schema(description = "Profundidade da lesão em cm")
        BigDecimal medidaProfundidade,

        @Schema(description = "Aspecto do leito da ferida")
        String aspectoLeitoFerida,

        @Schema(description = "Volume do exsudato")
        ExsudatoVolume exsudatoVolume,

        @Schema(description = "Característica do exsudato")
        ExsudatoCaracteristica exsudatoCaracteristica,

        @Schema(description = "Condição das bordas da lesão")
        String condicaoBordas,

        @Schema(description = "Aspecto da pele perilesional")
        String aspectoPerilesional,

        @Schema(description = "Presença de sinais flogísticos")
        Boolean sinaisFlogisticos,

        @Schema(description = "Presença de odor")
        Boolean presencaOdor,

        @Schema(description = "Limpeza realizada e solução utilizada")
        String limpezaRealizada,

        @Schema(description = "Coberturas aplicadas")
        String coberturasAplicadas,

        @Schema(description = "Produtos utilizados")
        String produtosUtilizados,

        @Schema(description = "Aceitação do procedimento pelo paciente")
        String aceitacaoProcedimento,

        @Schema(description = "Escala de dor EVA (0 a 10)")
        Integer escalaDor,

        @Schema(description = "Intercorrências durante o procedimento")
        String intercorrencias,

        @Schema(description = "Cuidados com o curativo orientados ao paciente")
        String cuidadosCurativo,

        @Schema(description = "Sinais de alerta orientados ao paciente")
        String sinaisAlerta,

        @Schema(description = "Orientações de retorno e acompanhamento")
        String orientacaoRetorno

) {}
