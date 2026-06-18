package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoTratamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.PacienteAgendamentosDTO;
import com.prontudigital.backend.agendamento.dto.ReagendarRequestDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.entidades.EvolucaoClinica;
import com.prontudigital.backend.agendamento.entidades.HistoricoAgendamento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;
import com.prontudigital.backend.agendamento.eventos.AgendamentoCanceladoEvento;
import com.prontudigital.backend.agendamento.eventos.AgendamentoCriadoEvento;
import com.prontudigital.backend.agendamento.eventos.AgendamentoReagendadoEvento;
import com.prontudigital.backend.agendamento.excecoes.*;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import com.prontudigital.backend.agendamento.repositorios.EvolucaoClinicaRepository;
import com.prontudigital.backend.agendamento.repositorios.HistoricoAgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.PacienteAgendamentoResumo;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.AgendamentoService;
import com.prontudigital.backend.agendamento.utils.AgendamentoUtil;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgendamentoServiceImpl implements AgendamentoService {

    private static final long DURACAO_MIN_MINUTOS = 15;
    private static final long DURACAO_MAX_MINUTOS = 480;

    private final AgendamentoRepository agendamentoRepository;
    private final EvolucaoClinicaRepository evolucaoClinicaRepository;
    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final HistoricoAgendamentoRepository historicoRepository;
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
                    "Usuario nao autorizado a visualizar este agendamento");
        }

        return agendamentoUtil.convertToDetalhadoDTO(agendamento);
    }

    @Override
    @Transactional
    public AgendamentoDetalhadoDTO atualizarObservacoes(Long id, String observacoes) {
        Agendamento agendamento = buscarOuFalhar(id);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        String perfil = permissaoPolicy.perfilEfetivo(usuario);
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    "Paciente nao pode editar observacoes de agendamento");
        }
        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    "Usuario nao autorizado a editar este agendamento");
        }

        agendamento.setObservacoes(observacoes);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        log.info("Observacoes atualizadas para agendamento id={}", salvo.getId());

        return agendamentoUtil.convertToDetalhadoDTO(salvo);
    }

    @Override
    @Transactional
    public AgendamentoDetalhadoDTO registrarEvolucao(Long id, EvolucaoTratamentoRequestDTO request) {
        Agendamento agendamento = buscarOuFalhar(id);
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        String perfil = permissaoPolicy.perfilEfetivo(usuario);
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    "Paciente nao pode registrar evolucao de tratamento");
        }
        if (!permissaoPolicy.podeModificar(usuario, agendamento)) {
            throw new UsuarioSemAutorizacaoException(
                    "Usuario nao autorizado a registrar evolucao neste agendamento");
        }
        /*if (agendamento.getTipo() != TipoAgendamento.TRATAMENTO) {
            throw new AgendamentoStatusInvalidoException(
                    "Evolucao clinica so pode ser registrada em agendamentos do tipo TRATAMENTO");
        }*/
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoStatusInvalidoException(
                    "Nao e possivel registrar evolucao em agendamento cancelado");
        }

        EvolucaoClinica evolucao = evolucaoClinicaRepository
                .findByAgendamento(agendamento)
                .orElseGet(() -> EvolucaoClinica.builder().agendamento(agendamento).build());

        // Dados da ferida
        evolucao.setDataAvaliacao(request.dataAvaliacao());
        evolucao.setLocalizacaoAnatomica(request.localizacaoAnatomica());
        evolucao.setEtiologia(request.etiologia());
        evolucao.setTempoEvolucao(request.tempoEvolucao());

        // Mensuração
        evolucao.setMedidaComprimento(request.medidaComprimento());
        evolucao.setMedidaLargura(request.medidaLargura());
        evolucao.setMedidaProfundidade(request.medidaProfundidade());
        evolucao.setTunelizacao(request.tunelizacao());
        evolucao.setDescolamentoBordas(request.descolamentoBordas());

        // Leito da ferida
        evolucao.setEpitelizacaoPercentual(request.epitelizacaoPercentual());
        evolucao.setGranulacaoPercentual(request.granulacaoPercentual());
        evolucao.setEsfaceloPercentual(request.esfaceloPercentual());
        evolucao.setNecrosePercentual(request.necrosePercentual());
        evolucao.setTendaoExposto(request.tendaoExposto());
        evolucao.setMusculoExposto(request.musculoExposto());
        evolucao.setOssoExposto(request.ossoExposto());

        // Exsudato
        evolucao.setExsudatoVolume(request.exsudatoVolume());
        evolucao.setExsudatoCaracteristica(request.exsudatoCaracteristica());
        evolucao.setOdorIntensidade(request.odorIntensidade());

        // Bordas / pele perilesional / sinais de infecção (múltipla escolha)
        evolucao.setCaracteristicasBordas(novoConjunto(request.caracteristicasBordas()));
        evolucao.setCaracteristicasPerilesional(novoConjunto(request.caracteristicasPerilesional()));
        evolucao.setSinaisInfeccao(novoConjunto(request.sinaisInfeccao()));

        // Dor
        evolucao.setClassificacaoDor(request.classificacaoDor());

        // Avaliação vascular
        evolucao.setGrauEdema(request.grauEdema());
        evolucao.setAvaliacaoPulsos(request.avaliacaoPulsos());

        // Evolução da ferida
        evolucao.setEvolucaoFerida(request.evolucaoFerida());
        evolucao.setSinaisEvolucao(novoConjunto(request.sinaisEvolucao()));

        // Conduta
        evolucao.setLimpezaLesao(request.limpezaLesao());
        evolucao.setDesbridamento(request.desbridamento());
        evolucao.setCoberturaAplicada(request.coberturaAplicada());
        evolucao.setCoberturaDescricao(request.coberturaDescricao());
        evolucao.setTerapiaAdjuvante(request.terapiaAdjuvante());
        evolucao.setTerapiaAdjuvanteDescricao(request.terapiaAdjuvanteDescricao());
        evolucao.setOrientacoesFornecidas(request.orientacoesFornecidas());

        // Observações
        evolucao.setObservacoes(request.observacoes());

        evolucaoClinicaRepository.save(evolucao);
        log.info("Evolucao clinica registrada para agendamento id={}", agendamento.getId());

        Agendamento atualizado = agendamentoRepository.findById(agendamento.getId())
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        "Agendamento nao encontrado: " + agendamento.getId()));
        return agendamentoUtil.convertToDetalhadoDTO(atualizado);
    }

    private static <T> Set<T> novoConjunto(Set<T> origem) {
        return origem == null ? new LinkedHashSet<>() : new LinkedHashSet<>(origem);
    }

    @Override
    @Transactional
    public AgendamentoResponseDTO agendar(AgendamentoRequestDTO request) {
        log.info("Agendando: paciente={} profissional={} inicio={}",
                request.pacienteUuid(), request.profissionalUuid(), request.inicioEm());

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeCriar(usuario, request)) {
            throw new UsuarioSemAutorizacaoException(
                    "Usuario nao autorizado a criar este agendamento");
        }

        usuarioService.validarUsuarioExiste(request.pacienteUuid());
        usuarioService.validarUsuarioExiste(request.profissionalUuid());

        validarPeriodo(request.inicioEm(), request.fimEm());
        validarDisponibilidade(request.profissionalUuid(), request.pacienteUuid(),
                request.inicioEm(), request.fimEm());
        validarRegraAvaliacaoTratamento(request);

        Agendamento agendamento = Agendamento.builder()
                .pacienteUuid(request.pacienteUuid())
                .profissionalUuid(request.profissionalUuid())
                .observacoes(request.observacoes())
                .inicioEm(request.inicioEm())
                .fimEm(request.fimEm())
                .status(StatusAgendamento.AGENDADO)
                .tipo(request.tipo())
                .tipoProcedimento(request.tipoProcedimento())
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
                    "Usuario nao autorizado a confirmar este agendamento");
        }
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new AgendamentoStatusInvalidoException(
                    "Apenas agendamentos com status AGENDADO podem ser confirmados");
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
                    "Usuario nao autorizado a cancelar este agendamento");
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoJaCanceladoException("Agendamento ja esta cancelado");
        }
        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new AgendamentoStatusInvalidoException(
                    "Agendamento concluido nao pode ser cancelado");
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
                    "Usuario nao autorizado a reagendar este agendamento");
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO ||
                agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new AgendamentoStatusInvalidoException(
                    "Agendamento " + agendamento.getStatus().name().toLowerCase() +
                            " nao pode ser reagendado");
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
                    "Usuario nao autorizado a concluir este agendamento");
        }
        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new AgendamentoJaConcluidoException("Agendamento ja esta concluido");
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoStatusInvalidoException(
                    "Agendamento cancelado nao pode ser concluido");
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
                    "Paciente nao pode visualizar agenda de profissional");
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
                    "Apenas pacientes podem acessar este endpoint");
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
                        "Avaliacao nao encontrada"));

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeVisualizar(usuario, avaliacao)) {
            throw new UsuarioSemAutorizacaoException(
                    "Usuario nao autorizado a ver tratamentos desta avaliacao");
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
                    "Paciente nao pode visualizar lista de pacientes");
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
                nomePaciente = "Paciente nao encontrado";
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
                    "Nao e possivel agendar para datas/horarios passados");
        }
        if (!fim.isAfter(inicio)) {
            throw new AgendamentoInvalidoException(
                    "Fim deve ser posterior ao inicio");
        }
        long minutos = Duration.between(inicio, fim).toMinutes();
        if (minutos < DURACAO_MIN_MINUTOS) {
            throw new AgendamentoInvalidoException(
                    "Duracao minima: " + DURACAO_MIN_MINUTOS + " minutos");
        }
        if (minutos > DURACAO_MAX_MINUTOS) {
            throw new AgendamentoInvalidoException(
                    "Duracao maxima: " + DURACAO_MAX_MINUTOS + " minutos");
        }
    }

    private void validarDisponibilidade(UUID profissionalUuid, UUID pacienteUuid,
                                        LocalDateTime inicio, LocalDateTime fim) {
        validarBloqueioHorario(profissionalUuid, inicio, fim);

        if (!agendamentoRepository
                .findConflitosParaProfissionalComLock(profissionalUuid, inicio, fim).isEmpty()) {
            throw new ProfissionalIndisponivelException(
                    "Profissional ja possui agendamento neste horario");
        }
        if (!agendamentoRepository
                .findConflitosParaPacienteComLock(pacienteUuid, inicio, fim).isEmpty()) {
            throw new PacienteIndisponivelException(
                    "Paciente ja possui agendamento neste horario");
        }
    }

    private void validarDisponibilidadeReagendamento(Agendamento agendamento,
                                                     LocalDateTime inicio,
                                                     LocalDateTime fim) {
        validarBloqueioHorario(agendamento.getProfissionalUuid(), inicio, fim);

        agendamentoRepository
                .findConflitosParaProfissionalComLock(
                        agendamento.getProfissionalUuid(), inicio, fim)
                .stream()
                .filter(a -> !a.getId().equals(agendamento.getId()))  // exclui o proprio
                .findFirst()
                .ifPresent(a -> {
                    throw new ProfissionalIndisponivelException(
                            "Profissional ja possui agendamento neste horario");
                });

        agendamentoRepository
                .findConflitosParaPacienteComLock(
                        agendamento.getPacienteUuid(), inicio, fim)
                .stream()
                .filter(a -> !a.getId().equals(agendamento.getId()))
                .findFirst()
                .ifPresent(a -> {
                    throw new PacienteIndisponivelException(
                            "Paciente ja possui agendamento neste horario");
                });
    }

    private void validarBloqueioHorario(UUID profissionalUuid,
                                        LocalDateTime inicio, LocalDateTime fim) {
        List<BloqueioHorario> bloqueios = bloqueioHorarioRepository
                .findConflitos(profissionalUuid, inicio, fim);

        if (!bloqueios.isEmpty()) {
            BloqueioHorario b = bloqueios.get(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            throw new HorarioIndisponivelException(String.format(
                    "Horario indisponivel. Bloqueio de %s ate %s. Motivo: %s",
                    b.getInicioEm().format(fmt),
                    b.getFimEm().format(fmt),
                    b.getMotivo()));
        }
    }

    private void validarRegraAvaliacaoTratamento(AgendamentoRequestDTO request) {
        if (request.tipo() == TipoAgendamento.AVALIACAO) {
            if (request.avaliacaoId() != null) {
                throw new AgendamentoInvalidoException("Avaliacao nao pode ter avaliacaoId");
            }
            return;
        }
        if (request.tipo() == TipoAgendamento.TRATAMENTO) {
            if (request.avaliacaoId() == null) {
                throw new AgendamentoInvalidoException(
                        "Tratamento deve estar associado a uma avaliacao");
            }
            Agendamento avaliacao = agendamentoRepository.findById(request.avaliacaoId())
                    .orElseThrow(() -> new AvaliacaoNaoEncontradaException(
                            "Avaliacao nao encontrada"));
            if (avaliacao.getTipo() != TipoAgendamento.AVALIACAO) {
                throw new AgendamentoInvalidoException(
                        "O agendamento referenciado nao e uma avaliacao");
            }
            if (!avaliacao.getPacienteUuid().equals(request.pacienteUuid())) {
                throw new AgendamentoInvalidoException(
                        "O paciente do tratamento deve ser o mesmo da avaliacao");
            }
            if (avaliacao.getStatus() != StatusAgendamento.REALIZADO) {
                throw new AgendamentoInvalidoException(
                        "A avaliacao deve estar concluida para agendar tratamentos");
            }
        }
    }

    private Agendamento buscarOuFalhar(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        "Agendamento nao encontrado: " + id));
    }

    private UUID exigirProfissionalUuid(UUID uuid) {
        if (uuid == null) {
            throw new AgendamentoInvalidoException(
                    "ADMIN deve informar profissionalUuid");
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