package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.AvaliacaoEvolucao;
import com.prontudigital.backend.agendamento.enums.BordasFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoTime;
import com.prontudigital.backend.agendamento.enums.InfeccaoInflamacao;
import com.prontudigital.backend.agendamento.enums.TecidoLeito;
import com.prontudigital.backend.agendamento.enums.TipoDesbridamento;
import com.prontudigital.backend.agendamento.enums.TipoFerida;
import com.prontudigital.backend.agendamento.enums.TipoLimpeza;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Dados da Ficha de Evolucao de Enfermagem (agendamentos do tipo AVALIACAO)")
@Builder
public record EvolucaoEnfermagemRequestDTO(

        // 1. Dados da avaliacao
        @Schema(description = "Data da avaliacao (pre-preenchida do agendamento)")
        LocalDate dataAvaliacao,

        @Schema(description = "Hora da avaliacao (pre-preenchida do agendamento)")
        LocalTime horaAvaliacao,

        @Size(max = 5000)
        @Schema(description = "Diagnostico medico")
        String diagnosticoMedico,

        @Schema(description = "Comorbidade: diabetes")
        Boolean comorbDiabetes,
        @Schema(description = "Comorbidade: hipertensao")
        Boolean comorbHipertensao,
        @Schema(description = "Comorbidade: doenca vascular")
        Boolean comorbDoencaVascular,
        @Schema(description = "Comorbidade: neuropatia")
        Boolean comorbNeuropatia,
        @Schema(description = "Comorbidade: outras")
        Boolean comorbOutras,
        @Size(max = 1000)
        @Schema(description = "Detalhe das outras comorbidades")
        String comorbOutrasDetalhe,

        @Size(max = 5000)
        @Schema(description = "Medicamentos relevantes em uso")
        String medicamentosRelevantes,

        // 2. Avaliacao da ferida (TIME)
        @Size(max = 5000)
        @Schema(description = "Localizacao anatomica da ferida")
        String localizacaoAnatomica,

        @Schema(description = "Tipo da ferida")
        TipoFerida tipoFerida,
        @Size(max = 255)
        @Schema(description = "Descricao quando tipo da ferida = OUTRA")
        String tipoFeridaOutra,

        @Size(max = 255)
        @Schema(description = "Dimensoes (texto livre)")
        String dimensoes,

        @Schema(description = "Comprimento em cm")
        BigDecimal comprimento,
        @Schema(description = "Largura em cm")
        BigDecimal largura,
        @Schema(description = "Profundidade em cm")
        BigDecimal profundidade,

        @Schema(description = "Presenca de tunelizacao")
        Boolean tunelizacao,
        @Schema(description = "Presenca de descolamento")
        Boolean descolamento,

        @Schema(description = "Tecido do leito da ferida (T)")
        TecidoLeito tecidoLeito,

        @Schema(description = "Infeccao/inflamacao (I)")
        InfeccaoInflamacao infeccaoInflamacao,

        @Schema(description = "Exsudato (M)")
        ExsudatoTime exsudato,
        @Size(max = 255)
        @Schema(description = "Tipo do exsudato")
        String exsudatoTipo,

        @Schema(description = "Bordas da ferida (E)")
        BordasFerida bordas,

        @Size(max = 5000)
        @Schema(description = "Aspecto da pele perilesional")
        String pelePerilesional,

        @Min(0) @Max(10)
        @Schema(description = "Escala de dor (0-10)")
        Integer dorEscala,

        @Size(max = 5000)
        @Schema(description = "Sinais vitais (texto livre)")
        String sinaisVitais,
        @Size(max = 30)
        @Schema(description = "Pressao arterial", example = "120x80")
        String pa,
        @Size(max = 30)
        @Schema(description = "Frequencia cardiaca", example = "78 bpm")
        String fc,
        @Size(max = 30)
        @Schema(description = "Frequencia respiratoria", example = "16 irpm")
        String fr,
        @Size(max = 30)
        @Schema(description = "Temperatura", example = "36,5 C")
        String temp,

        // 3. Diagnosticos de enfermagem
        @Schema(description = "Integridade da pele prejudicada")
        Boolean diagIntegridadePele,
        @Schema(description = "Integridade tissular prejudicada")
        Boolean diagIntegridadeTissular,
        @Schema(description = "Risco de infeccao")
        Boolean diagRiscoInfeccao,
        @Schema(description = "Perfusao tissular ineficaz")
        Boolean diagPerfusaoIneficaz,
        @Schema(description = "Dor aguda")
        Boolean diagDorAguda,
        @Size(max = 5000)
        @Schema(description = "Outros diagnosticos de enfermagem")
        String diagOutros,

        // 4. Conduta realizada
        @Schema(description = "Solucao de limpeza utilizada")
        TipoLimpeza limpeza,
        @Size(max = 255)
        @Schema(description = "Descricao quando limpeza = OUTRO")
        String limpezaOutro,

        @Schema(description = "Tipo de desbridamento")
        TipoDesbridamento desbridamento,

        @Size(max = 1000)
        @Schema(description = "Cobertura primaria aplicada")
        String coberturaPrimaria,
        @Size(max = 1000)
        @Schema(description = "Cobertura secundaria aplicada")
        String coberturaSecundaria,
        @Size(max = 1000)
        @Schema(description = "Fixacao utilizada")
        String fixacao,
        @Size(max = 5000)
        @Schema(description = "Orientacoes fornecidas ao paciente")
        String orientacoesPaciente,

        // 5. Avaliacao da evolucao
        @Schema(description = "Avaliacao da evolucao da ferida")
        AvaliacaoEvolucao avaliacaoEvolucao,
        @Schema(description = "Houve reducao da area da ferida")
        Boolean reducaoArea,
        @Size(max = 5000)
        @Schema(description = "Observacoes")
        String observacoes,

        // 6. Plano
        @Schema(description = "Plano: manter conduta")
        Boolean planoManterConduta,
        @Schema(description = "Plano: ajustar cobertura")
        Boolean planoAjustarCobertura,
        @Schema(description = "Plano: solicitar avaliacao medica")
        Boolean planoAvaliacaoMedica,
        @Schema(description = "Plano: solicitar exames")
        Boolean planoSolicitarExames,
        @Schema(description = "Plano: encaminhamento")
        Boolean planoEncaminhamento,
        @Min(0)
        @Schema(description = "Retorno em N dias")
        Integer retornoDias

) {}
