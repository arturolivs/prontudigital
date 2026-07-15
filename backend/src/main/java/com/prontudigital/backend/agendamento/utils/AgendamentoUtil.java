package com.prontudigital.backend.agendamento.utils;

import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoClinicaDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoClinica;
import com.prontudigital.backend.agendamento.entidades.Procedimento;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgendamentoUtil {

    private final UsuarioService usuarioService;

    public AgendamentoResponseDTO convertToResponseDTO(Agendamento agendamento) {
        return new AgendamentoResponseDTO(
                agendamento.getId(),
                agendamento.getInicioEm(),
                agendamento.getFimEm(),
                agendamento.getProfissionalUuid(),
                agendamento.getPacienteUuid(),
                agendamento.getTipo(),
                agendamento.getTipoProcedimento(),
                procedimentoId(agendamento),
                procedimentoNome(agendamento),
                agendamento.getLocalAtendimento(),
                agendamento.getPacienteAcamado(),
                agendamento.getStatus(),
                agendamento.getCriadoEm(),
                agendamento.getAvaliacao() != null ? agendamento.getAvaliacao().getId() : null,
                agendamento.getConcluidoEm()
        );
    }

    public AgendamentoViewDTO convertToViewDTO(Agendamento agendamento) {
        String nomePaciente;
        String nomeProfissional;

        try {
            UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
            UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());

            nomePaciente = paciente != null ? paciente.nomeCompleto() : "Paciente nao encontrado";
            nomeProfissional = profissional != null ? profissional.nomeCompleto() : "Profissional nao encontrado";
        } catch (Exception e) {
            log.error("Erro ao resolver nomes para agendamento id={}", agendamento.getId(), e);
            nomePaciente = "Indisponivel";
            nomeProfissional = "Indisponivel";
        }

        return new AgendamentoViewDTO(
                agendamento.getId(),
                agendamento.getInicioEm(),
                agendamento.getFimEm(),
                agendamento.getProfissionalUuid(),
                agendamento.getPacienteUuid(),
                agendamento.getTipo(),
                agendamento.getTipoProcedimento(),
                procedimentoId(agendamento),
                procedimentoNome(agendamento),
                agendamento.getLocalAtendimento(),
                agendamento.getPacienteAcamado(),
                agendamento.getStatus(),
                nomePaciente,
                nomeProfissional,
                agendamento.getAvaliacao() != null ? agendamento.getAvaliacao().getId() : null
        );
    }

    public AgendamentoDetalhadoDTO convertToDetalhadoDTO(Agendamento agendamento) {
        String nomePaciente;
        String nomeProfissional;

        try {
            UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
            UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());
            nomePaciente = paciente.nomeCompleto();
            nomeProfissional = profissional.nomeCompleto();
        } catch (Exception e) {
            log.error("Erro ao resolver nomes para agendamento id={}", agendamento.getId(), e);
            nomePaciente = "Indisponivel";
            nomeProfissional = "Indisponivel";
        }

        return new AgendamentoDetalhadoDTO(
                agendamento.getId(),
                agendamento.getInicioEm(),
                agendamento.getFimEm(),
                agendamento.getProfissionalUuid(),
                agendamento.getPacienteUuid(),
                agendamento.getTipo(),
                agendamento.getTipoProcedimento(),
                procedimentoId(agendamento),
                procedimentoNome(agendamento),
                agendamento.getLocalAtendimento(),
                agendamento.getPacienteAcamado(),
                agendamento.getStatus(),
                nomePaciente,
                nomeProfissional,
                agendamento.getCriadoEm(),
                agendamento.getAvaliacao() != null ? agendamento.getAvaliacao().getId() : null,
                agendamento.getConcluidoEm(),
                toEvolucaoDTO(agendamento.getEvolucaoClinica())
        );
    }

    private Long procedimentoId(Agendamento agendamento) {
        Procedimento p = agendamento.getProcedimento();
        return p != null ? p.getId() : null;
    }

    private String procedimentoNome(Agendamento agendamento) {
        Procedimento p = agendamento.getProcedimento();
        return p != null ? p.getNome() : null;
    }

    private EvolucaoClinicaDTO toEvolucaoDTO(EvolucaoClinica ec) {
        if (ec == null) return null;
        return new EvolucaoClinicaDTO(
                ec.getLocalizacaoAnatomica(),
                ec.getEtiologia(),
                ec.getTempoEvolucao(),
                ec.getMedidaComprimento(),
                ec.getMedidaLargura(),
                ec.getMedidaProfundidade(),
                ec.getTunelizacao(),
                ec.getDescolamentoBordas(),
                ec.getEpitelizacaoPercentual(),
                ec.getGranulacaoPercentual(),
                ec.getEsfaceloPercentual(),
                ec.getNecrosePercentual(),
                ec.getTendaoExposto(),
                ec.getMusculoExposto(),
                ec.getOssoExposto(),
                ec.getExsudatoVolume(),
                ec.getExsudatoCaracteristica(),
                ec.getOdorIntensidade(),
                ec.getCaracteristicasBordas(),
                ec.getCaracteristicasPerilesional(),
                ec.getSinaisInfeccao(),
                ec.getClassificacaoDor(),
                ec.getGrauEdema(),
                ec.getAvaliacaoPulsos(),
                ec.getEvolucaoFerida(),
                ec.getSinaisEvolucao(),
                ec.getLimpezaLesao(),
                ec.getDesbridamento(),
                ec.getCoberturaAplicada(),
                ec.getCoberturaDescricao(),
                ec.getTerapiaAdjuvante(),
                ec.getTerapiaAdjuvanteDescricao(),
                ec.getOrientacoesFornecidas(),
                ec.getObservacoes()
        );
    }
}
