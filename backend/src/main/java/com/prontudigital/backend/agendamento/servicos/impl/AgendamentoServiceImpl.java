package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoCurativoRequestDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoEnfermagemRequestDTO;
import com.prontudigital.backend.agendamento.dto.PacienteAgendamentosDTO;
import com.prontudigital.backend.agendamento.dto.ReagendarRequestDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.entidades.EvolucaoCurativo;
import com.prontudigital.backend.agendamento.entidades.EvolucaoEnfermagem;
import com.prontudigital.backend.agendamento.entidades.HistoricoAgendamento;
import com.prontudigital.backend.agendamento.entidades.Procedimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;
import com.prontudigital.backend.agendamento.eventos.AgendamentoCanceladoEvento;
import com.prontudigital.backend.agendamento.eventos.AgendamentoCriadoEvento;
import com.prontudigital.backend.agendamento.eventos.AgendamentoReagendadoEvento;
import com.prontudigital.backend.agendamento.excecoes.*;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import com.prontudigital.backend.agendamento.repositorios.HorarioTrabalhoRepository;
import com.prontudigital.backend.agendamento.excecoes.ForaDoHorarioTrabalhoException;
import com.prontudigital.backend.agendamento.repositorios.EvolucaoCurativoRepository;
import com.prontudigital.backend.agendamento.repositorios.EvolucaoEnfermagemRepository;
import com.prontudigital.backend.agendamento.repositorios.HistoricoAgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.PacienteAgendamentoResumo;
import com.prontudigital.backend.agendamento.repositorios.ProcedimentoRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.AgendamentoService;
import com.prontudigital.backend.agendamento.utils.AgendamentoUtil;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgendamentoServiceImpl implements AgendamentoService {

    private static final long DURACAO_MIN_MINUTOS = 15;
    private static final long DURACAO_MAX_MINUTOS = 480;
    private static final long ANTECEDENCIA_MIN_CANCELAMENTO_HORAS = 24;

    private final AgendamentoRepository agendamentoRepository;
    private final EvolucaoEnfermagemRepository evolucaoEnfermagemRepository;
    private final EvolucaoCurativoRepository evolucaoCurativoRepository;
    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final HistoricoAgendamentoRepository historicoRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;
    private final AgendamentoUtil agendamentoUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public AgendamentoDetalhadoDTO buscarPorId(Long id) {
        Agendamento agendamento = buscarOuFalhar(id);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeVisualizar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.visualizar"));
        }

        return agendamentoUtil.convertToDetalhadoDTO(agendamento);
    }

    @Override
    @Transactional
    public AgendamentoDetalhadoDTO registrarEvolucaoEnfermagem(Long id,
                                                               EvolucaoEnfermagemRequestDTO request) {
        Agendamento agendamento = buscarOuFalhar(id);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        String perfil = permissaoPolicy.perfilEfetivo(usuario);
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.paciente.nao-registra-evolucao"));
        }
        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.registrar-evolucao"));
        }
        if (agendamento.getTipo() != TipoAgendamento.AVALIACAO) {
            throw new AgendamentoStatusInvalidoException(
                    Mensagens.get("agendamento.evolucao-enfermagem.apenas-avaliacao"));
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoStatusInvalidoException(
                    Mensagens.get("agendamento.evolucao.cancelado"));
        }

        EvolucaoEnfermagem evolucao = evolucaoEnfermagemRepository
                .findByAgendamento(agendamento)
                .orElseGet(() -> EvolucaoEnfermagem.builder().agendamento(agendamento).build());

        aplicarEvolucaoEnfermagem(evolucao, request);

        evolucaoEnfermagemRepository.save(evolucao);
        log.info("Evolucao de enfermagem registrada para agendamento id={}", agendamento.getId());

        Agendamento atualizado = agendamentoRepository.findById(agendamento.getId())
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("agendamento.nao-encontrado", agendamento.getId())));
        return agendamentoUtil.convertToDetalhadoDTO(atualizado);
    }

    private void aplicarEvolucaoEnfermagem(EvolucaoEnfermagem evolucao,
                                           EvolucaoEnfermagemRequestDTO request) {
        // 1. Dados da avaliacao
        evolucao.setDataAvaliacao(request.dataAvaliacao());
        evolucao.setHoraAvaliacao(request.horaAvaliacao());
        evolucao.setDiagnosticoMedico(request.diagnosticoMedico());
        evolucao.setComorbDiabetes(request.comorbDiabetes());
        evolucao.setComorbHipertensao(request.comorbHipertensao());
        evolucao.setComorbDoencaVascular(request.comorbDoencaVascular());
        evolucao.setComorbNeuropatia(request.comorbNeuropatia());
        evolucao.setComorbOutras(request.comorbOutras());
        evolucao.setComorbOutrasDetalhe(request.comorbOutrasDetalhe());
        evolucao.setMedicamentosRelevantes(request.medicamentosRelevantes());

        // 2. Avaliacao da ferida (TIME)
        evolucao.setLocalizacaoAnatomica(request.localizacaoAnatomica());
        evolucao.setTipoFerida(request.tipoFerida());
        evolucao.setTipoFeridaOutra(request.tipoFeridaOutra());
        evolucao.setDimensoes(request.dimensoes());
        evolucao.setComprimento(request.comprimento());
        evolucao.setLargura(request.largura());
        evolucao.setProfundidade(request.profundidade());
        evolucao.setTunelizacao(request.tunelizacao());
        evolucao.setDescolamento(request.descolamento());
        evolucao.setTecidoLeito(request.tecidoLeito());
        evolucao.setInfeccaoInflamacao(request.infeccaoInflamacao());
        evolucao.setExsudato(request.exsudato());
        evolucao.setExsudatoTipo(request.exsudatoTipo());
        evolucao.setBordas(request.bordas());
        evolucao.setPelePerilesional(request.pelePerilesional());
        evolucao.setDorEscala(request.dorEscala());
        evolucao.setSinaisVitais(request.sinaisVitais());
        evolucao.setPa(request.pa());
        evolucao.setFc(request.fc());
        evolucao.setFr(request.fr());
        evolucao.setTemp(request.temp());

        // 3. Diagnosticos de enfermagem
        evolucao.setDiagIntegridadePele(request.diagIntegridadePele());
        evolucao.setDiagIntegridadeTissular(request.diagIntegridadeTissular());
        evolucao.setDiagRiscoInfeccao(request.diagRiscoInfeccao());
        evolucao.setDiagPerfusaoIneficaz(request.diagPerfusaoIneficaz());
        evolucao.setDiagDorAguda(request.diagDorAguda());
        evolucao.setDiagOutros(request.diagOutros());

        // 4. Conduta realizada
        evolucao.setLimpeza(request.limpeza());
        evolucao.setLimpezaOutro(request.limpezaOutro());
        evolucao.setDesbridamento(request.desbridamento());
        evolucao.setCoberturaPrimaria(request.coberturaPrimaria());
        evolucao.setCoberturaSecundaria(request.coberturaSecundaria());
        evolucao.setFixacao(request.fixacao());
        evolucao.setOrientacoesPaciente(request.orientacoesPaciente());

        // 5. Avaliacao da evolucao
        evolucao.setAvaliacaoEvolucao(request.avaliacaoEvolucao());
        evolucao.setReducaoArea(request.reducaoArea());
        evolucao.setObservacoes(request.observacoes());

        // 6. Plano
        evolucao.setPlanoManterConduta(request.planoManterConduta());
        evolucao.setPlanoAjustarCobertura(request.planoAjustarCobertura());
        evolucao.setPlanoAvaliacaoMedica(request.planoAvaliacaoMedica());
        evolucao.setPlanoSolicitarExames(request.planoSolicitarExames());
        evolucao.setPlanoEncaminhamento(request.planoEncaminhamento());
        evolucao.setRetornoDias(request.retornoDias());
    }

    @Override
    @Transactional
    public AgendamentoDetalhadoDTO registrarEvolucaoCurativo(Long id,
                                                             EvolucaoCurativoRequestDTO request) {
        Agendamento agendamento = buscarOuFalhar(id);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        String perfil = permissaoPolicy.perfilEfetivo(usuario);
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.paciente.nao-registra-evolucao"));
        }
        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.registrar-evolucao"));
        }
        if (agendamento.getTipo() != TipoAgendamento.TRATAMENTO) {
            throw new AgendamentoStatusInvalidoException(
                    Mensagens.get("agendamento.evolucao-curativo.apenas-tratamento"));
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoStatusInvalidoException(
                    Mensagens.get("agendamento.evolucao.cancelado"));
        }

        EvolucaoCurativo evolucao = evolucaoCurativoRepository
                .findByAgendamento(agendamento)
                .orElseGet(() -> EvolucaoCurativo.builder().agendamento(agendamento).build());

        aplicarEvolucaoCurativo(evolucao, request);

        evolucaoCurativoRepository.save(evolucao);
        log.info("Evolucao diaria de curativos registrada para agendamento id={}",
                agendamento.getId());

        Agendamento atualizado = agendamentoRepository.findById(agendamento.getId())
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("agendamento.nao-encontrado", agendamento.getId())));
        return agendamentoUtil.convertToDetalhadoDTO(atualizado);
    }

    private void aplicarEvolucaoCurativo(EvolucaoCurativo evolucao,
                                         EvolucaoCurativoRequestDTO request) {
        // 1. Avaliacao diaria
        evolucao.setComprimento(request.comprimento());
        evolucao.setLargura(request.largura());
        evolucao.setProfundidade(request.profundidade());
        evolucao.setAreaAproximada(request.areaAproximada());
        evolucao.setTecido(request.tecido());
        evolucao.setInfeccaoInflamacao(request.infeccaoInflamacao());
        evolucao.setExsudato(request.exsudato());
        evolucao.setBordas(request.bordas());
        evolucao.setOdorPresente(request.odorPresente());
        evolucao.setDorEscala(request.dorEscala());
        evolucao.setPelePerilesional(request.pelePerilesional());

        // 2. Intervencoes
        evolucao.setLimpezaIrrigacao(request.limpezaIrrigacao());
        evolucao.setDesbridamento(request.desbridamento());
        evolucao.setDesbridamentoObs(request.desbridamentoObs());
        evolucao.setCoberturaPrimaria(request.coberturaPrimaria());
        evolucao.setOrientacoesPaciente(request.orientacoesPaciente());

        // 3. Avaliacao da evolucao
        evolucao.setEvolucao(request.evolucao());
        evolucao.setObservacoes(request.observacoes());

        // 4. Plano / acoes futuras
        evolucao.setPlanoManterConduta(request.planoManterConduta());
        evolucao.setPlanoAlterarCobertura(request.planoAlterarCobertura());
        evolucao.setPlanoSolicitarExames(request.planoSolicitarExames());
        evolucao.setPlanoEncaminhamento(request.planoEncaminhamento());
        evolucao.setRetornoPrevisto(request.retornoPrevisto());
    }

    @Override
    @Transactional
    public AgendamentoResponseDTO agendar(AgendamentoRequestDTO request) {
        log.info("Agendando: paciente={} profissional={} inicio={}",
                request.pacienteUuid(), request.profissionalUuid(), request.inicioEm());

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeCriar(usuario, request)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.criar"));
        }

        usuarioService.validarUsuarioExiste(request.pacienteUuid());
        usuarioService.validarUsuarioExiste(request.profissionalUuid());

        validarPeriodo(request.inicioEm(), request.fimEm());
        validarDisponibilidade(request.profissionalUuid(), request.pacienteUuid(),
                request.inicioEm(), request.fimEm());
        validarRegraAvaliacaoTratamento(request);

        Procedimento procedimento = resolverProcedimento(request);

        Agendamento agendamento = Agendamento.builder()
                .pacienteUuid(request.pacienteUuid())
                .profissionalUuid(request.profissionalUuid())
                .inicioEm(request.inicioEm())
                .fimEm(request.fimEm())
                .status(StatusAgendamento.AGENDADO)
                .tipo(request.tipo())
                .procedimento(procedimento)
                .tipoProcedimento(tipoProcedimentoLegado(procedimento))
                .localAtendimento(request.localAtendimento())
                .pacienteAcamado(request.pacienteAcamado())
                .build();

        if (request.tipo() == TipoAgendamento.TRATAMENTO) {
            agendamento.setAvaliacao(
                    agendamentoRepository.getReferenceById(request.avaliacaoId()));
        }

        Agendamento salvo = agendamentoRepository.save(agendamento);
        log.info("Agendamento criado. ID: {}", salvo.getId());

        eventPublisher.publishEvent(new AgendamentoCriadoEvento(salvo));
        return agendamentoUtil.convertToResponseDTO(salvo);
    }

    @Override
    @Transactional
    public void confirmar(Long agendamentoId) {
        Agendamento agendamento = buscarOuFalhar(agendamentoId);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.confirmar"));
        }
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new AgendamentoStatusInvalidoException(
                    Mensagens.get("agendamento.status.apenas-agendado-confirma"));
        }

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(agendamento);
        log.info("Agendamento {} confirmado pelo usuario {}", agendamentoId, usuario.uuid());
    }

    @Override
    @Transactional
    public void cancelar(Long agendamentoId) {
        Agendamento agendamento = buscarOuFalhar(agendamentoId);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.cancelar"));
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoJaCanceladoException(Mensagens.get("agendamento.ja-cancelado"));
        }
        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new AgendamentoStatusInvalidoException(
                    Mensagens.get("agendamento.concluido-nao-cancela"));
        }

        boolean isAdmin = "ADMIN".equals(permissaoPolicy.perfilEfetivo(usuario));
        if (!isAdmin) {
            LocalDateTime limiteCancelamento = agendamento.getInicioEm()
                    .minusHours(ANTECEDENCIA_MIN_CANCELAMENTO_HORAS);
            if (!LocalDateTime.now(clock).isBefore(limiteCancelamento)) {
                throw new CancelamentoForaDoPrazoException(Mensagens.get(
                        "agendamento.cancelamento.fora-prazo", ANTECEDENCIA_MIN_CANCELAMENTO_HORAS));
            }
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);

        eventPublisher.publishEvent(new AgendamentoCanceladoEvento(agendamento));
    }

    @Override
    @Transactional
    public AgendamentoResponseDTO reagendar(Long agendamentoId, ReagendarRequestDTO request) {
        Agendamento agendamento = buscarOuFalhar(agendamentoId);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.reagendar"));
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO ||
                agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new AgendamentoStatusInvalidoException(Mensagens.get(
                    "agendamento.status-invalido.reagendar",
                    agendamento.getStatus().name().toLowerCase()));
        }

        validarPeriodo(request.novoInicioEm(), request.novoFimEm());

        validarDisponibilidadeReagendamento(
                agendamento, request.novoInicioEm(), request.novoFimEm());

        HistoricoAgendamento historico = HistoricoAgendamento.builder()
                .agendamentoId(agendamento.getId())
                .inicioAnterior(agendamento.getInicioEm())
                .fimAnterior(agendamento.getFimEm())
                .inicioNovo(request.novoInicioEm())
                .fimNovo(request.novoFimEm())
                .motivo(request.motivo())
                .alteradoPor(usuario.uuid())
                .build();
        historicoRepository.save(historico);

        LocalDateTime inicioAnterior = agendamento.getInicioEm();
        LocalDateTime fimAnterior = agendamento.getFimEm();

        agendamento.setInicioEm(request.novoInicioEm());
        agendamento.setFimEm(request.novoFimEm());
        Agendamento salvo = agendamentoRepository.save(agendamento);

        log.info("Agendamento {} reagendado para {} - {}",
                salvo.getId(), salvo.getInicioEm(), salvo.getFimEm());

        eventPublisher.publishEvent(
                new AgendamentoReagendadoEvento(salvo, inicioAnterior, fimAnterior));

        return agendamentoUtil.convertToResponseDTO(salvo);
    }

    @Override
    @Transactional
    public void concluir(Long agendamentoId) {
        Agendamento agendamento = buscarOuFalhar(agendamentoId);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.concluir"));
        }
        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new AgendamentoJaConcluidoException(Mensagens.get("agendamento.ja-concluido"));
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoStatusInvalidoException(
                    Mensagens.get("agendamento.cancelado-nao-conclui"));
        }

        agendamento.setStatus(StatusAgendamento.REALIZADO);
        agendamento.setConcluidoEm(LocalDateTime.now(clock));
        agendamentoRepository.save(agendamento);
    }

    @Override
    public List<AgendamentoViewDTO> visualizarAgenda(LocalDate data,
                                                     TipoVisualizacaoAgenda tipo,
                                                     UUID profissionalUuidOpcional) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.paciente.nao-visualiza-agenda"));
        }

        UUID profissionalUuid = "ADMIN".equals(perfil)
                ? exigirProfissionalUuid(profissionalUuidOpcional)
                : usuario.uuid();

        Periodo periodo = calcularPeriodo(data, tipo);

        return agendamentoRepository
                .findByProfissionalUuidAndInicioEmBetween(
                        profissionalUuid, periodo.inicio(), periodo.fim())
                .stream()
                .map(agendamentoUtil::convertToViewDTO)
                .toList();
    }

    @Override
    public List<AgendamentoViewDTO> obterMeusAgendamentos() {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if (!"PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.apenas-pacientes-endpoint"));
        }

        return agendamentoRepository
                .findByPacienteUuidOrderByInicioEmDesc(usuario.uuid())
                .stream()
                .map(agendamentoUtil::convertToViewDTO)
                .toList();
    }

    @Override
    public List<AgendamentoViewDTO> getTratamentosPorAvaliacao(Long avaliacaoId) {
        Agendamento avaliacao = agendamentoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new AvaliacaoNaoEncontradaException(
                        Mensagens.get("avaliacao.nao-encontrada")));

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeVisualizar(usuario, avaliacao)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.nao-autorizado.ver-tratamentos"));
        }

        List<AgendamentoViewDTO> historico = new ArrayList<>();
        historico.add(agendamentoUtil.convertToViewDTO(avaliacao));
        agendamentoRepository.findByAvaliacaoId(avaliacaoId).stream()
                .map(agendamentoUtil::convertToViewDTO)
                .forEach(historico::add);
        return historico;
    }

    @Override
    public Page<PacienteAgendamentosDTO> listarPacientesComAgendamentos(
            String busca, StatusAgendamento status, Pageable pageable) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("agendamento.paciente.nao-visualiza-lista-pacientes"));
        }

        LocalDate hoje = LocalDate.now(clock);
        LocalDateTime inicio = hoje.withDayOfMonth(1).minusMonths(2).atStartOfDay();
        LocalDateTime fim = hoje.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        String buscaNormalizada = (busca == null || busca.isBlank()) ? null : busca.trim();
        Pageable paginacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<PacienteAgendamentoResumo> pagina = agendamentoRepository
                .buscarPacientesComAgendamentos(inicio, fim, status, buscaNormalizada, paginacao);

        return pagina.map(resumo -> {
            UUID pacienteUuid = resumo.getPacienteUuid();
            String nomePaciente;
            try {
                nomePaciente = usuarioService.buscarPorUuid(pacienteUuid).nomeCompleto();
            } catch (Exception e) {
                nomePaciente = Mensagens.get("paciente.nao-encontrado");
            }

            List<AgendamentoViewDTO> agendamentos = agendamentoRepository
                    .findByPacienteEPeriodo(pacienteUuid, inicio, fim, status)
                    .stream()
                    .map(agendamentoUtil::convertToViewDTO)
                    .toList();

            return new PacienteAgendamentosDTO(pacienteUuid, nomePaciente, agendamentos);
        });
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new AgendamentoDataHoraInvalidaException(
                    Mensagens.get("agendamento.data-passada"));
        }
        if (!fim.isAfter(inicio)) {
            throw new AgendamentoInvalidoException(
                    Mensagens.get("agendamento.fim-antes-inicio"));
        }
        long minutos = Duration.between(inicio, fim).toMinutes();
        if (minutos < DURACAO_MIN_MINUTOS) {
            throw new AgendamentoInvalidoException(
                    Mensagens.get("agendamento.duracao-minima", DURACAO_MIN_MINUTOS));
        }
        if (minutos > DURACAO_MAX_MINUTOS) {
            throw new AgendamentoInvalidoException(
                    Mensagens.get("agendamento.duracao-maxima", DURACAO_MAX_MINUTOS));
        }
    }

    private void validarDisponibilidade(UUID profissionalUuid, UUID pacienteUuid,
                                        LocalDateTime inicio, LocalDateTime fim) {
        validarHorarioTrabalho(profissionalUuid, inicio, fim);
        validarBloqueioHorario(profissionalUuid, inicio, fim);

        if (!agendamentoRepository
                .findConflitosParaProfissionalComLock(profissionalUuid, inicio, fim).isEmpty()) {
            throw new ProfissionalIndisponivelException(
                    Mensagens.get("agendamento.profissional-indisponivel"));
        }
        if (!agendamentoRepository
                .findConflitosParaPacienteComLock(pacienteUuid, inicio, fim).isEmpty()) {
            throw new PacienteIndisponivelException(
                    Mensagens.get("agendamento.paciente-indisponivel"));
        }
    }

    private void validarDisponibilidadeReagendamento(Agendamento agendamento,
                                                     LocalDateTime inicio,
                                                     LocalDateTime fim) {
        validarHorarioTrabalho(agendamento.getProfissionalUuid(), inicio, fim);
        validarBloqueioHorario(agendamento.getProfissionalUuid(), inicio, fim);

        agendamentoRepository
                .findConflitosParaProfissionalComLock(
                        agendamento.getProfissionalUuid(), inicio, fim)
                .stream()
                .filter(a -> !a.getId().equals(agendamento.getId()))  // exclui o proprio
                .findFirst()
                .ifPresent(a -> {
                    throw new ProfissionalIndisponivelException(
                            Mensagens.get("agendamento.profissional-indisponivel"));
                });

        agendamentoRepository
                .findConflitosParaPacienteComLock(
                        agendamento.getPacienteUuid(), inicio, fim)
                .stream()
                .filter(a -> !a.getId().equals(agendamento.getId()))
                .findFirst()
                .ifPresent(a -> {
                    throw new PacienteIndisponivelException(
                            Mensagens.get("agendamento.paciente-indisponivel"));
                });
    }

    /**
     * RF05 — o atendimento precisa caber inteiro em uma janela de expediente do
     * profissional naquele dia da semana.
     *
     * <p>Profissional sem nenhuma janela cadastrada nao e validado: a regra so
     * passa a valer depois que alguem define o expediente, para nao invalidar os
     * profissionais que ja existiam antes do RF05.
     */
    private void validarHorarioTrabalho(UUID profissionalUuid,
                                        LocalDateTime inicio, LocalDateTime fim) {
        if (!horarioTrabalhoRepository.existsByProfissionalUuidAndAtivoTrue(profissionalUuid)) {
            return;
        }

        // Um atendimento que vira o dia nunca cabe numa janela de um dia so.
        boolean mesmoDia = inicio.toLocalDate().equals(fim.toLocalDate());

        boolean dentroDoExpediente = mesmoDia && horarioTrabalhoRepository
                .findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                        profissionalUuid, inicio.getDayOfWeek().getValue())
                .stream()
                .anyMatch(h -> h.contem(inicio.toLocalTime(), fim.toLocalTime()));

        if (!dentroDoExpediente) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            throw new ForaDoHorarioTrabalhoException(Mensagens.get(
                    "agendamento.fora-do-horario-trabalho",
                    inicio.format(fmt),
                    fim.format(fmt)));
        }
    }

    private void validarBloqueioHorario(UUID profissionalUuid,
                                        LocalDateTime inicio, LocalDateTime fim) {
        List<BloqueioHorario> bloqueios = bloqueioHorarioRepository
                .findConflitos(profissionalUuid, inicio, fim);

        if (!bloqueios.isEmpty()) {
            BloqueioHorario b = bloqueios.get(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            throw new HorarioIndisponivelException(Mensagens.get(
                    "agendamento.horario-indisponivel",
                    b.getInicioEm().format(fmt),
                    b.getFimEm().format(fmt),
                    b.getMotivo()));
        }
    }

    private void validarRegraAvaliacaoTratamento(AgendamentoRequestDTO request) {
        if (request.tipo() == TipoAgendamento.AVALIACAO) {
            if (request.avaliacaoId() != null) {
                throw new AgendamentoInvalidoException(Mensagens.get("agendamento.avaliacao.sem-avaliacao-id"));
            }
            return;
        }
        if (request.tipo() == TipoAgendamento.TRATAMENTO) {
            if (request.avaliacaoId() == null) {
                throw new AgendamentoInvalidoException(
                        Mensagens.get("agendamento.tratamento.exige-avaliacao"));
            }
            Agendamento avaliacao = agendamentoRepository.findById(request.avaliacaoId())
                    .orElseThrow(() -> new AvaliacaoNaoEncontradaException(
                            Mensagens.get("avaliacao.nao-encontrada")));
            if (avaliacao.getTipo() != TipoAgendamento.AVALIACAO) {
                throw new AgendamentoInvalidoException(
                        Mensagens.get("agendamento.tratamento.ref-nao-avaliacao"));
            }
            if (!avaliacao.getPacienteUuid().equals(request.pacienteUuid())) {
                throw new AgendamentoInvalidoException(
                        Mensagens.get("agendamento.tratamento.paciente-diferente"));
            }
            if (avaliacao.getStatus() != StatusAgendamento.REALIZADO) {
                throw new AgendamentoInvalidoException(
                        Mensagens.get("agendamento.tratamento.avaliacao-nao-concluida"));
            }
        }
    }

    /**
     * RF06: resolve o procedimento a partir do id (preferido) ou do enum legado
     * tipoProcedimento, mantendo compatibilidade durante a migracao.
     */
    private Procedimento resolverProcedimento(AgendamentoRequestDTO request) {
        if (request.procedimentoId() != null) {
            Procedimento procedimento = procedimentoRepository.findById(request.procedimentoId())
                    .orElseThrow(() -> new ProcedimentoNaoEncontradoException(
                            Mensagens.get("procedimento.nao-encontrado", request.procedimentoId())));
            if (!Boolean.TRUE.equals(procedimento.getAtivo())) {
                throw new AgendamentoInvalidoException(Mensagens.get("procedimento.inativo"));
            }
            return procedimento;
        }
        if (request.tipoProcedimento() != null) {
            return procedimentoRepository.findByCodigo(request.tipoProcedimento().name())
                    .orElseThrow(() -> new ProcedimentoNaoEncontradoException(
                            Mensagens.get("procedimento.nao-encontrado", request.tipoProcedimento())));
        }
        throw new AgendamentoInvalidoException(
                Mensagens.get("agendamento.procedimento-obrigatorio"));
    }

    private TipoProcedimento tipoProcedimentoLegado(Procedimento procedimento) {
        if (procedimento.getCodigo() == null) return null;
        try {
            return TipoProcedimento.valueOf(procedimento.getCodigo());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Agendamento buscarOuFalhar(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("agendamento.nao-encontrado", id)));
    }

    private UUID exigirProfissionalUuid(UUID uuid) {
        if (uuid == null) {
            throw new AgendamentoInvalidoException(
                    Mensagens.get("agendamento.admin.informar-profissional"));
        }
        return uuid;
    }

    private Periodo calcularPeriodo(LocalDate data, TipoVisualizacaoAgenda tipo) {
        return switch (tipo) {
            case DIA -> new Periodo(data.atStartOfDay(), data.atTime(LocalTime.MAX));
            case SEMANA -> {
                LocalDateTime inicio = data
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .atStartOfDay();
                yield new Periodo(inicio, inicio.plusDays(6).with(LocalTime.MAX));
            }
            case MES -> new Periodo(
                    data.withDayOfMonth(1).atStartOfDay(),
                    data.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX)
            );
        };
    }

    private record Periodo(LocalDateTime inicio, LocalDateTime fim) {}
}