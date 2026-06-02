package com.prontudigital.backend.agendamento.dto;

import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados completos do agendamento")
public record AgendamentoResponseDTO(

        Long id,
        LocalDateTime inicioEm,
        LocalDateTime fimEm,
        UUID profissionalUuid,
        UUID pacienteUuid,
        TipoAgendamento tipo,
        TipoProcedimento tipoProcedimento,
        LocalAtendimento localAtendimento,
        Boolean pacienteAcamado,
        StatusAgendamento status,
        String observacoes,
        LocalDateTime criadoEm,
        Long avaliacaoId,
        LocalDateTime concluidoEm,

        String localizacaoAnatomica,
        String tipoLesao,
        BigDecimal medidaComprimento,
        BigDecimal medidaLargura,
        BigDecimal medidaProfundidade,
        String aspectoLeitoFerida,
        ExsudatoVolume exsudatoVolume,
        ExsudatoCaracteristica exsudatoCaracteristica,
        String condicaoBordas,
        String aspectoPerilesional,
        Boolean sinaisFlogisticos,
        Boolean presencaOdor,

        String limpezaRealizada,
        String coberturasAplicadas,
        String produtosUtilizados,

        String aceitacaoProcedimento,
        Integer escalaDor,
        String intercorrencias,

        String cuidadosCurativo,
        String sinaisAlerta,
        String orientacaoRetorno

) {}