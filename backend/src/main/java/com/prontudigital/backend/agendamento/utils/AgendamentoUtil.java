package com.prontudigital.backend.agendamento.utils;

import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoCurativoDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoEnfermagemDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoCurativo;
import com.prontudigital.backend.agendamento.entidades.EvolucaoEnfermagem;
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
                toEvolucaoEnfermagemDTO(agendamento.getEvolucaoEnfermagem()),
                toEvolucaoCurativoDTO(agendamento.getEvolucaoCurativo())
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

    private EvolucaoEnfermagemDTO toEvolucaoEnfermagemDTO(EvolucaoEnfermagem ee) {
        if (ee == null) return null;
        return EvolucaoEnfermagemDTO.builder()
                .dataAvaliacao(ee.getDataAvaliacao())
                .horaAvaliacao(ee.getHoraAvaliacao())
                .diagnosticoMedico(ee.getDiagnosticoMedico())
                .comorbDiabetes(ee.getComorbDiabetes())
                .comorbHipertensao(ee.getComorbHipertensao())
                .comorbDoencaVascular(ee.getComorbDoencaVascular())
                .comorbNeuropatia(ee.getComorbNeuropatia())
                .comorbOutras(ee.getComorbOutras())
                .comorbOutrasDetalhe(ee.getComorbOutrasDetalhe())
                .medicamentosRelevantes(ee.getMedicamentosRelevantes())
                .localizacaoAnatomica(ee.getLocalizacaoAnatomica())
                .tipoFerida(ee.getTipoFerida())
                .tipoFeridaOutra(ee.getTipoFeridaOutra())
                .dimensoes(ee.getDimensoes())
                .comprimento(ee.getComprimento())
                .largura(ee.getLargura())
                .profundidade(ee.getProfundidade())
                .tunelizacao(ee.getTunelizacao())
                .descolamento(ee.getDescolamento())
                .tecidoLeito(ee.getTecidoLeito())
                .infeccaoInflamacao(ee.getInfeccaoInflamacao())
                .exsudato(ee.getExsudato())
                .exsudatoTipo(ee.getExsudatoTipo())
                .bordas(ee.getBordas())
                .pelePerilesional(ee.getPelePerilesional())
                .dorEscala(ee.getDorEscala())
                .sinaisVitais(ee.getSinaisVitais())
                .pa(ee.getPa())
                .fc(ee.getFc())
                .fr(ee.getFr())
                .temp(ee.getTemp())
                .diagIntegridadePele(ee.getDiagIntegridadePele())
                .diagIntegridadeTissular(ee.getDiagIntegridadeTissular())
                .diagRiscoInfeccao(ee.getDiagRiscoInfeccao())
                .diagPerfusaoIneficaz(ee.getDiagPerfusaoIneficaz())
                .diagDorAguda(ee.getDiagDorAguda())
                .diagOutros(ee.getDiagOutros())
                .limpeza(ee.getLimpeza())
                .limpezaOutro(ee.getLimpezaOutro())
                .desbridamento(ee.getDesbridamento())
                .coberturaPrimaria(ee.getCoberturaPrimaria())
                .coberturaSecundaria(ee.getCoberturaSecundaria())
                .fixacao(ee.getFixacao())
                .orientacoesPaciente(ee.getOrientacoesPaciente())
                .avaliacaoEvolucao(ee.getAvaliacaoEvolucao())
                .reducaoArea(ee.getReducaoArea())
                .observacoes(ee.getObservacoes())
                .planoManterConduta(ee.getPlanoManterConduta())
                .planoAjustarCobertura(ee.getPlanoAjustarCobertura())
                .planoAvaliacaoMedica(ee.getPlanoAvaliacaoMedica())
                .planoSolicitarExames(ee.getPlanoSolicitarExames())
                .planoEncaminhamento(ee.getPlanoEncaminhamento())
                .retornoDias(ee.getRetornoDias())
                .criadoEm(ee.getCriadoEm())
                .atualizadoEm(ee.getAtualizadoEm())
                .build();
    }

    private EvolucaoCurativoDTO toEvolucaoCurativoDTO(EvolucaoCurativo ec) {
        if (ec == null) return null;
        return EvolucaoCurativoDTO.builder()
                .comprimento(ec.getComprimento())
                .largura(ec.getLargura())
                .profundidade(ec.getProfundidade())
                .areaAproximada(ec.getAreaAproximada())
                .tecido(ec.getTecido())
                .infeccaoInflamacao(ec.getInfeccaoInflamacao())
                .exsudato(ec.getExsudato())
                .bordas(ec.getBordas())
                .odorPresente(ec.getOdorPresente())
                .dorEscala(ec.getDorEscala())
                .pelePerilesional(ec.getPelePerilesional())
                .limpezaIrrigacao(ec.getLimpezaIrrigacao())
                .desbridamento(ec.getDesbridamento())
                .desbridamentoObs(ec.getDesbridamentoObs())
                .coberturaPrimaria(ec.getCoberturaPrimaria())
                .orientacoesPaciente(ec.getOrientacoesPaciente())
                .evolucao(ec.getEvolucao())
                .observacoes(ec.getObservacoes())
                .planoManterConduta(ec.getPlanoManterConduta())
                .planoAlterarCobertura(ec.getPlanoAlterarCobertura())
                .planoSolicitarExames(ec.getPlanoSolicitarExames())
                .planoEncaminhamento(ec.getPlanoEncaminhamento())
                .retornoPrevisto(ec.getRetornoPrevisto())
                .criadoEm(ec.getCriadoEm())
                .atualizadoEm(ec.getAtualizadoEm())
                .build();
    }
}
