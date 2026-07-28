package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.AvaliacaoEvolucao;
import com.prontudigital.backend.agendamento.enums.BordasFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoTime;
import com.prontudigital.backend.agendamento.enums.InfeccaoInflamacaoCurativo;
import com.prontudigital.backend.agendamento.enums.TecidoLeito;
import com.prontudigital.backend.agendamento.enums.TipoDesbridamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Schema(description = "Dados da Ficha de Evolucao Diaria - Curativos (agendamentos do tipo TRATAMENTO)")
@Builder
public record EvolucaoCurativoRequestDTO(

        // 1. Avaliacao diaria
        @Schema(description = "Comprimento em cm")
        BigDecimal comprimento,
        @Schema(description = "Largura em cm")
        BigDecimal largura,
        @Schema(description = "Profundidade em cm")
        BigDecimal profundidade,
        @Schema(description = "Area aproximada em cm2 (C x L, calculada no frontend)")
        BigDecimal areaAproximada,

        @Schema(description = "Tecido predominante no leito (T)")
        TecidoLeito tecido,

        @Schema(description = "Infeccao/inflamacao (I) - legenda 0/1/2")
        InfeccaoInflamacaoCurativo infeccaoInflamacao,

        @Schema(description = "Exsudato (M) - legenda 0/1/2/3")
        ExsudatoTime exsudato,

        @Schema(description = "Bordas da ferida (E)")
        BordasFerida bordas,

        @Schema(description = "Presenca de odor")
        Boolean odorPresente,

        @Min(0) @Max(10)
        @Schema(description = "Escala de dor (0-10)")
        Integer dorEscala,

        @Size(max = 5000)
        @Schema(description = "Aspecto da pele perilesional")
        String pelePerilesional,

        // 2. Intervencoes
        @Size(max = 5000)
        @Schema(description = "Limpeza/irrigacao realizada")
        String limpezaIrrigacao,

        @Schema(description = "Tipo de desbridamento")
        TipoDesbridamento desbridamento,
        @Size(max = 5000)
        @Schema(description = "Observacoes do desbridamento")
        String desbridamentoObs,

        @Size(max = 1000)
        @Schema(description = "Cobertura primaria aplicada")
        String coberturaPrimaria,

        @Size(max = 5000)
        @Schema(description = "Orientacoes fornecidas ao paciente")
        String orientacoesPaciente,

        // 3. Avaliacao da evolucao
        @Schema(description = "Avaliacao da evolucao da ferida")
        AvaliacaoEvolucao evolucao,
        @Size(max = 5000)
        @Schema(description = "Observacoes")
        String observacoes,

        // 4. Plano / acoes futuras (observacao preenchida = acao marcada)
        @Size(max = 1000)
        @Schema(description = "Plano: manter conduta (observacao)")
        String planoManterConduta,
        @Size(max = 1000)
        @Schema(description = "Plano: alterar cobertura (observacao)")
        String planoAlterarCobertura,
        @Size(max = 1000)
        @Schema(description = "Plano: solicitar exames (observacao)")
        String planoSolicitarExames,
        @Size(max = 1000)
        @Schema(description = "Plano: encaminhamento (observacao)")
        String planoEncaminhamento,
        @Size(max = 1000)
        @Schema(description = "Retorno previsto (observacao)")
        String retornoPrevisto

) {}
