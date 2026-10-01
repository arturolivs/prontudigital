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
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Ficha de Evolucao de Enfermagem registrada na avaliacao")
@Builder
public record EvolucaoEnfermagemDTO(

        // 1. Dados da avaliacao
        LocalDate dataAvaliacao,
        LocalTime horaAvaliacao,
        String diagnosticoMedico,
        Boolean comorbDiabetes,
        Boolean comorbHipertensao,
        Boolean comorbDoencaVascular,
        Boolean comorbNeuropatia,
        Boolean comorbOutras,
        String comorbOutrasDetalhe,
        String medicamentosRelevantes,

        // 2. Avaliacao da ferida (TIME)
        String localizacaoAnatomica,
        TipoFerida tipoFerida,
        String tipoFeridaOutra,
        String dimensoes,
        BigDecimal comprimento,
        BigDecimal largura,
        BigDecimal profundidade,
        Boolean tunelizacao,
        Boolean descolamento,
        TecidoLeito tecidoLeito,
        InfeccaoInflamacao infeccaoInflamacao,
        ExsudatoTime exsudato,
        String exsudatoTipo,
        BordasFerida bordas,
        String pelePerilesional,
        Integer dorEscala,
        String sinaisVitais,
        String pa,
        String fc,
        String fr,
        String temp,

        // 3. Diagnosticos de enfermagem
        Boolean diagIntegridadePele,
        Boolean diagIntegridadeTissular,
        Boolean diagRiscoInfeccao,
        Boolean diagPerfusaoIneficaz,
        Boolean diagDorAguda,
        String diagOutros,

        // 4. Conduta realizada
        TipoLimpeza limpeza,
        String limpezaOutro,
        TipoDesbridamento desbridamento,
        String coberturaPrimaria,
        String coberturaSecundaria,
        String fixacao,
        String orientacoesPaciente,

        // 5. Avaliacao da evolucao
        AvaliacaoEvolucao avaliacaoEvolucao,
        Boolean reducaoArea,
        String observacoes,

        // 6. Plano
        Boolean planoManterConduta,
        Boolean planoAjustarCobertura,
        Boolean planoAvaliacaoMedica,
        Boolean planoSolicitarExames,
        Boolean planoEncaminhamento,
        Integer retornoDias,

        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm

) {}
