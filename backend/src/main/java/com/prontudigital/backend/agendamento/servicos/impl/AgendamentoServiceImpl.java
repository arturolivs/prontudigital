package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgendamentoServiceImpl implements AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final UsuarioService usuarioService;         // injeção direta — substituiu UserServiceClient
    private final AgendamentoUtil agendamentoUtil;

    // ========== PERMISSÕES ==========

    private void validarPermissaoCriacao(AgendamentoRequestDTO request,
                                         UUID usuarioUuid, String role) {
        if ("ADMIN".equals(role)) return;

        if ("PACIENTE".equals(role)) {
            if (!request.pacienteUuid().equals(usuarioUuid)) {
                throw new UnauthorizedException("Paciente so pode criar agendamentos para si mesmo");
            }
            return;
        }
        if ("PROFISSIONAL".equals(role)) {
            if (!request.profissionalUuid().equals(usuarioUuid)) {
                throw new UnauthorizedException("Profissional so pode criar agendamentos para si mesmo");
            }
            return;
        }
        throw new UnauthorizedException("Usuario nao autorizado a criar agendamentos");
    }

    private boolean temPermissaoParaModificar(Agendamento agendamento,
                                              UUID usuarioUuid, String role) {
        if ("ADMIN".equals(role)) return true;
        if ("PACIENTE".equals(role)) return agendamento.getPacienteUuid().equals(usuarioUuid);
        if ("PROFISSIONAL".equals(role)) return agendamento.getProfissionalUuid().equals(usuarioUuid);
        return false;
    }

    private boolean temPermissaoParaVisualizar(Agendamento agendamento,
                                               UUID usuarioUuid, String role) {
        if ("ADMIN".equals(role)) return true;
        if ("PACIENTE".equals(role)) return agendamento.getPacienteUuid().equals(usuarioUuid);
        if ("PROFISSIONAL".equals(role)) return agendamento.getProfissionalUuid().equals(usuarioUuid);
        return false;
    }

    // ========== VALIDAÇÕES DE DISPONIBILIDADE ==========

    private void validarDataFutura(LocalDateTime dataHora) {
        if (dataHora.isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentTimeException("Nao e possivel agendar para datas/horarios passados");
        }
    }

    private void validarDisponibilidadeProfissional(UUID profissionalUuid,
                                                    LocalDateTime inicio,
                                                    LocalDateTime fim) {
        List<BloqueioHorario> bloqueios = bloqueioHorarioRepository
                .findConflitos(profissionalUuid, inicio, fim);

        if (!bloqueios.isEmpty()) {
            BloqueioHorario conflito = bloqueios.get(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            throw new TimeBlockConflictException(
                    String.format("Horario indisponivel. Bloqueio de %s ate %s. Motivo: %s",
                            conflito.getInicioEm().format(fmt),
                            conflito.getFimEm().format(fmt),
                            conflito.getMotivo())
            );
        }

        List<Agendamento> conflitos = agendamentoRepository
                .findConflitosParaProfissional(profissionalUuid, inicio, fim);
        if (!conflitos.isEmpty()) {
            throw new ProfessionalNotAvailableException(
                    "Profissional ja possui agendamento neste horario");
        }
    }

    private void validarDisponibilidadePaciente(UUID pacienteUuid,
                                                LocalDateTime inicio,
                                                LocalDateTime fim) {
        List<Agendamento> conflitos = agendamentoRepository
                .findConflitosParaPaciente(pacienteUuid, inicio, fim);
        if (!conflitos.isEmpty()) {
            throw new PatientNotAvailableException(
                    "Paciente ja possui agendamento neste horario. Conflitos: " + conflitos.size());
        }
    }

    // ========== CASOS DE USO ==========

    @Override
    @Transactional
    public AgendamentoResponseDTO agendar(AgendamentoRequestDTO request) {
        log.info("Agendando consulta: paciente={} profissional={} inicio={}",
                request.pacienteUuid(), request.profissionalUuid(), request.inicioEm());

        UsuarioDTO usuario = usuarioService.getUsuarioAtual();
        validarPermissaoCriacao(request, usuario.uuid(), usuario.roles().iterator().next());

        usuarioService.validarUsuarioExiste(request.pacienteUuid());
        usuarioService.validarUsuarioExiste(request.profissionalUuid());
        validarDataFutura(request.inicioEm());
        validarDisponibilidadeProfissional(request.profissionalUuid(), request.inicioEm(), request.fimEm());
        validarDisponibilidadePaciente(request.pacienteUuid(), request.inicioEm(), request.fimEm());

        if (request.tipo() == TipoAgendamento.TRATAMENTO) {
            if (request.avaliacaoId() == null) {
                throw new InvalidAppointmentRequestException("Tratamento deve estar associado a uma avaliacao");
            }
            Agendamento avaliacao = agendamentoRepository.findById(request.avaliacaoId())
                    .orElseThrow(() -> new EvaluationNotFoundException("Avaliacao nao encontrada"));
            if (avaliacao.getTipo() != TipoAgendamento.AVALIACAO) {
                throw new InvalidAppointmentRequestException("O agendamento referenciado nao e uma avaliacao");
            }
            if (!avaliacao.getPacienteUuid().equals(request.pacienteUuid())) {
                throw new InvalidAppointmentRequestException("O paciente do tratamento deve ser o mesmo da avaliacao");
            }
            if (avaliacao.getStatus() != StatusAgendamento.CONCLUIDO) {
                throw new InvalidAppointmentRequestException("A avaliacao deve estar concluida para agendar tratamentos");
            }
        } else if (request.tipo() == TipoAgendamento.AVALIACAO && request.avaliacaoId() != null) {
            throw new InvalidAppointmentRequestException("Avaliacao nao pode ter avaliacaoId");
        }

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
            agendamento.setAvaliacao(agendamentoRepository.getReferenceById(request.avaliacaoId()));
        }

        Agendamento salvo = agendamentoRepository.save(agendamento);
        log.info("Agendamento criado com sucesso. ID: {}", salvo.getId());
        return agendamentoUtil.convertToResponseDTO(salvo);
    }

    @Override
    @Transactional
    public void cancelar(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new AppointmentNotFoundException("Agendamento nao encontrado"));

        UsuarioDTO usuario = usuarioService.getUsuarioAtual();

        if (!temPermissaoParaModificar(agendamento, usuario.uuid(), usuario.roles().iterator().next())) {
            throw new UnauthorizedException("Usuario nao autorizado a cancelar este agendamento");
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AppointmentAlreadyCancelledException("Agendamento ja esta cancelado");
        }
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new InvalidAppointmentStateException("Agendamento concluido nao pode ser cancelado");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    @Override
    @Transactional
    public void concluir(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new AppointmentNotFoundException("Agendamento nao encontrado"));

        UsuarioDTO usuario = usuarioService.getUsuarioAtual();

        if (!temPermissaoParaModificar(agendamento, usuario.uuid(), usuario.roles().iterator().next())) {
            throw new UnauthorizedException("Usuario nao autorizado a concluir este agendamento");
        }
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new AppointmentAlreadyCompletedException("Agendamento ja esta concluido");
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new InvalidAppointmentStateException("Agendamento cancelado nao pode ser concluido");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamento.setConcluidoEm(LocalDateTime.now());
        agendamentoRepository.save(agendamento);
    }

    @Override
    public List<AgendamentoViewDTO> visualizarAgenda(LocalDate data, String tipoVisualizacao) {
        UsuarioDTO usuario = usuarioService.getUsuarioAtual();
        String role = usuario.roles().iterator().next();

        if ("PACIENTE".equals(role)) {
            throw new UnauthorizedException("Paciente nao pode visualizar agenda de profissional");
        }

        LocalDateTime inicio;
        LocalDateTime fim;

        switch (tipoVisualizacao.toLowerCase()) {
            case "dia" -> {
                inicio = data.atStartOfDay();
                fim = data.atTime(23, 59, 59);
            }
            case "semana" -> {
                inicio = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                fim = inicio.plusDays(6).with(LocalTime.of(23, 59, 59));
            }
            case "mes" -> {
                inicio = data.withDayOfMonth(1).atStartOfDay();
                fim = data.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
            }
            default -> throw new InvalidViewTypeException("Tipo de visualizacao invalido: " + tipoVisualizacao);
        }

        return agendamentoRepository
                .findByProfissionalUuidAndInicioEmBetween(usuario.uuid(), inicio, fim)
                .stream()
                .map(agendamentoUtil::convertToViewDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgendamentoViewDTO> getTratamentosPorAvaliacao(Long avaliacaoId) {
        Agendamento avaliacao = agendamentoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new EvaluationNotFoundException("Avaliacao nao encontrada"));

        UsuarioDTO usuario = usuarioService.getUsuarioAtual();

        if (!temPermissaoParaVisualizar(avaliacao, usuario.uuid(), usuario.roles().iterator().next())) {
            throw new UnauthorizedException("Usuario nao autorizado a ver tratamentos desta avaliacao");
        }

        return agendamentoRepository.findByAvaliacaoId(avaliacaoId)
                .stream()
                .map(agendamentoUtil::convertToViewDTO)
                .collect(Collectors.toList());
    }
}