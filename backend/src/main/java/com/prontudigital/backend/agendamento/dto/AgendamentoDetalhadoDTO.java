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

@Schema(description = "Dados completos do agendamento com nomes resolvidos e evolução clínica")
public record AgendamentoDetalhadoDTO(

        @Schema(description = "ID interno do agendamento")
        Long id,

        @Schema(description = "Data e hora de início")
        LocalDateTime inicioEm,

        @Schema(description = "Data e hora de término")
        LocalDateTime fimEm,

        @Schema(description = "UUID do profissional")
        UUID profissionalUuid,

        @Schema(description = "UUID do paciente")
        UUID pacienteUuid,

        @Schema(description = "Tipo: AVALIACAO ou TRATAMENTO")
        TipoAgendamento tipo,

        @Schema(description = "Tipo de procedimento: PODIATRIA ou TRATAMENTO_FERIDAS")
        TipoProcedimento tipoProcedimento,

        @Schema(description = "Local do atendimento: CLINICA ou RESIDENCIAL")
        LocalAtendimento localAtendimento,

        @Schema(description = "Indica se o paciente esta acamado")
        Boolean pacienteAcamado,

        @Schema(description = "Status atual do agendamento")
        StatusAgendamento status,

        @Schema(description = "Nome completo do paciente (resolvido)")
        String nomePaciente,

        @Schema(description = "Nome completo do profissional (resolvido)")
        String nomeProfissional,

        @Schema(description = "Observações clínicas registradas")
        String observacoes,

        @Schema(description = "Data de criação do registro")
        LocalDateTime criadoEm,

        @Schema(description = "ID da avaliação vinculada (apenas para TRATAMENTO)")
        Long avaliacaoId,

        @Schema(description = "Data e hora em que o agendamento foi concluído")
        LocalDateTime concluidoEm,

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

        @Schema(description = "Aspecto do leito da ferida (granulação, epitelização, esfacelo, necrose)")
        String aspectoLeitoFerida,

        @Schema(description = "Volume do exsudato: AUSENTE, PEQUENO, MODERADO ou GRANDE")
        ExsudatoVolume exsudatoVolume,

        @Schema(description = "Característica do exsudato: SEROSO, SEROSSANGUINOLENTO ou PURULENTO")
        ExsudatoCaracteristica exsudatoCaracteristica,

        @Schema(description = "Condição das bordas da lesão")
        String condicaoBordas,

        @Schema(description = "Aspecto da pele perilesional")
        String aspectoPerilesional,

        @Schema(description = "Presença de sinais flogísticos (dor, calor, rubor, edema)")
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
