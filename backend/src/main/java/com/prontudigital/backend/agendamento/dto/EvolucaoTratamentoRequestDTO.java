package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

@Schema(description = "Dados da evolução clínica de enfermagem pós-curativo")
public record EvolucaoTratamentoRequestDTO(

        // Seção 2 – Avaliação da lesão
        @Schema(description = "Localização anatômica da lesão")
        String localizacaoAnatomica,

        @Schema(description = "Tipo de lesão (LPP, úlcera venosa, pé diabético etc.)")
        String tipoLesao,

        @Schema(description = "Comprimento da lesão em cm", example = "3.5")
        BigDecimal medidaComprimento,

        @Schema(description = "Largura da lesão em cm", example = "2.0")
        BigDecimal medidaLargura,

        @Schema(description = "Profundidade da lesão em cm", example = "0.5")
        BigDecimal medidaProfundidade,

        @Schema(description = "Aspecto do leito da ferida (granulação, epitelização, esfacelo, necrose)")
        String aspectoLeitoFerida,

        @Schema(description = "Volume do exsudato")
        ExsudatoVolume exsudatoVolume,

        @Schema(description = "Característica do exsudato")
        ExsudatoCaracteristica exsudatoCaracteristica,

        @Schema(description = "Condição das bordas da lesão")
        String condicaoBordas,

        @Schema(description = "Aspecto da pele perilesional")
        String aspectoPerilesional,

        @Schema(description = "Presença de sinais flogísticos (dor, calor, rubor, edema)")
        Boolean sinaisFlogisticos,

        @Schema(description = "Presença de odor")
        Boolean presencaOdor,

        // Seção 3 – Procedimento realizado
        @Schema(description = "Limpeza realizada e solução utilizada")
        String limpezaRealizada,

        @Schema(description = "Coberturas aplicadas")
        String coberturasAplicadas,

        @Schema(description = "Produtos utilizados")
        String produtosUtilizados,

        // Seção 4 – Resposta do paciente
        @Schema(description = "Aceitação do procedimento pelo paciente")
        String aceitacaoProcedimento,

        @Schema(description = "Escala de dor EVA (0 a 10)", example = "4")
        @Min(0) @Max(10)
        Integer escalaDor,

        @Schema(description = "Intercorrências durante o procedimento")
        String intercorrencias,

        // Seção 5 – Orientações fornecidas
        @Schema(description = "Cuidados com o curativo orientados ao paciente")
        String cuidadosCurativo,

        @Schema(description = "Sinais de alerta orientados ao paciente")
        String sinaisAlerta,

        @Schema(description = "Orientações de retorno e acompanhamento")
        String orientacaoRetorno

) {}
