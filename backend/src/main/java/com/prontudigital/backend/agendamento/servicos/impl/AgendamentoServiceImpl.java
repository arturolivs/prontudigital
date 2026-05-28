package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.*;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
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
import com.prontudigital.backend.agendamento.repositorios.HistoricoAgendamentoRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgendamentoServiceImpl implements AgendamentoService {

    private static final long DURACAO_MIN_MINUTOS = 15;
    private static final long DURACAO_MAX_MINUTOS = 480;

    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final HistoricoAgendamentoRepository historicoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;
    private final AgendamentoUtil agendamentoUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

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

        return agendamentoRepository.findByAvaliacaoId(avaliacaoId)
                .stream()
                .map(agendamentoUtil::convertToViewDTO)
                .toList();
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