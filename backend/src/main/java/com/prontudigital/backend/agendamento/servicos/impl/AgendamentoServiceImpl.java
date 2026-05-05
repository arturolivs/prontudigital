package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.excecoes.*;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import com.prontudigital.backend.agendamento.servicos.AgendamentoService;
import com.prontudigital.backend.agendamento.utils.AgendamentoUtil;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.servicos.AutenticacaoService;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
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
    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacaoService;
    private final AgendamentoUtil agendamentoUtil;

    private void validarPermissaoCriacao(AgendamentoRequestDTO request,
                                         UUID usuarioUuid, String perfil) {
        if ("ADMIN".equals(perfil)) return;

        if ("PACIENTE".equals(perfil)) {
            if (!request.pacienteUuid().equals(usuarioUuid)) {
                throw new UsuarioSemAutorizacaoException("Paciente so pode criar agendamentos para si mesmo");
            }
            return;
        }
        if ("PROFISSIONAL".equals(perfil)) {
            if (!request.profissionalUuid().equals(usuarioUuid)) {
                throw new UsuarioSemAutorizacaoException("Profissional so pode criar agendamentos para si mesmo");
            }
            return;
        }
        throw new UsuarioSemAutorizacaoException("Usuário nao autorizado a criar agendamentos");
    }

    private boolean temPermissaoParaModificar(Agendamento agendamento,
                                              UUID usuarioUuid, String perfil) {
        if ("ADMIN".equals(perfil)) return true;
        if ("PACIENTE".equals(perfil)) return agendamento.getPacienteUuid().equals(usuarioUuid);
        if ("PROFISSIONAL".equals(perfil)) return agendamento.getProfissionalUuid().equals(usuarioUuid);
        return false;
    }

    private boolean temPermissaoParaVisualizar(Agendamento agendamento,
                                               UUID usuarioUuid, String perfil) {
        if ("ADMIN".equals(perfil)) return true;
        if ("PACIENTE".equals(perfil)) return agendamento.getPacienteUuid().equals(usuarioUuid);
        if ("PROFISSIONAL".equals(perfil)) return agendamento.getProfissionalUuid().equals(usuarioUuid);
        return false;
    }

    private void validarDataFutura(LocalDateTime dataHora) {
        if (dataHora.isBefore(LocalDateTime.now())) {
            throw new AgendamentoDataHoraInvalidaException("Nao e possível agendar para datas/horários passados");
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
            throw new HorarioIndisponivelException(
                    String.format("Horário indisponível. Bloqueio de %s ate %s. Motivo: %s",
                            conflito.getInicioEm().format(fmt),
                            conflito.getFimEm().format(fmt),
                            conflito.getMotivo())
            );
        }

        List<Agendamento> conflitos = agendamentoRepository
                .findConflitosParaProfissional(profissionalUuid, inicio, fim);
        if (!conflitos.isEmpty()) {
            throw new ProfessionalndisponivelException(
                    "Profissional ja possui agendamento neste horário");
        }
    }

    private void validarDisponibilidadePaciente(UUID pacienteUuid,
                                                LocalDateTime inicio,
                                                LocalDateTime fim) {
        List<Agendamento> conflitos = agendamentoRepository
                .findConflitosParaPaciente(pacienteUuid, inicio, fim);
        if (!conflitos.isEmpty()) {
            throw new PacienteIndisponivelException(
                    "Paciente ja possui agendamento neste horário. Conflitos: " + conflitos.size());
        }
    }

    @Override
    @Transactional
    public AgendamentoResponseDTO agendar(AgendamentoRequestDTO request) {
        log.info("Agendando consulta: paciente={} profissional={} inicio={}",
                request.pacienteUuid(), request.profissionalUuid(), request.inicioEm());

        UsuarioDTO usuario = autenticacaoService.getUsuarioAtual();
        validarPermissaoCriacao(request, usuario.uuid(), usuario.perfis().iterator().next());

        usuarioService.validarUsuarioExiste(request.pacienteUuid());
        usuarioService.validarUsuarioExiste(request.profissionalUuid());
        validarDataFutura(request.inicioEm());
        validarDisponibilidadeProfissional(request.profissionalUuid(), request.inicioEm(), request.fimEm());
        validarDisponibilidadePaciente(request.pacienteUuid(), request.inicioEm(), request.fimEm());

        if (request.tipo() == TipoAgendamento.TRATAMENTO) {
            if (request.avaliacaoId() == null) {
                throw new AgendamentoInvalidoException("Tratamento deve estar associado a uma avaliação");
            }
            Agendamento avaliacao = agendamentoRepository.findById(request.avaliacaoId())
                    .orElseThrow(() -> new AvaliacaoNaoEncontradaException("Avaliação nao encontrada"));
            if (avaliacao.getTipo() != TipoAgendamento.AVALIACAO) {
                throw new AgendamentoInvalidoException("O agendamento referenciado não e uma avaliação");
            }
            if (!avaliacao.getPacienteUuid().equals(request.pacienteUuid())) {
                throw new AgendamentoInvalidoException("O paciente do tratamento deve ser o mesmo da avaliação");
            }
            if (avaliacao.getStatus() != StatusAgendamento.CONCLUIDO) {
                throw new AgendamentoInvalidoException("A avaliação deve estar concluída para agendar tratamentos");
            }
        } else if (request.tipo() == TipoAgendamento.AVALIACAO && request.avaliacaoId() != null) {
            throw new AgendamentoInvalidoException("Avaliação nao pode ter avaliacaoId");
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
                .orElseThrow(() -> new AgendamentoNaoEncontradoException("Agendamento não encontrado"));

        UsuarioDTO usuario = autenticacaoService.getUsuarioAtual();

        if (!temPermissaoParaModificar(agendamento, usuario.uuid(), usuario.perfis().iterator().next())) {
            throw new UsuarioSemAutorizacaoException("Usuário nao autorizado a cancelar este agendamento");
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoCanceladoException("Agendamento ja esta cancelado");
        }
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new AgendamentoStatusInvalidoException("Agendamento concluído nao pode ser cancelado");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    @Override
    @Transactional
    public void concluir(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException("Agendamento nao encontrado"));

        UsuarioDTO usuario = autenticacaoService.getUsuarioAtual();

        if (!temPermissaoParaModificar(agendamento, usuario.uuid(), usuario.perfis().iterator().next())) {
            throw new UsuarioSemAutorizacaoException("Usuário nao autorizado a concluir este agendamento");
        }
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new AgendamentoJaConcluidoException("Agendamento ja esta concluído");
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new AgendamentoStatusInvalidoException("Agendamento cancelado não pode ser concluído");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamento.setConcluidoEm(LocalDateTime.now());
        agendamentoRepository.save(agendamento);
    }

    @Override
    public List<AgendamentoViewDTO> visualizarAgenda(LocalDate data, String tipoVisualizacao) {
        UsuarioDTO usuario = autenticacaoService.getUsuarioAtual();
        String perfil = usuario.perfis().iterator().next();

        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException("Paciente não pode visualizar agenda de profissional");
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
            default -> throw new TipoVisualizacaoInvalidoException("Tipo de visualização invalido: " + tipoVisualizacao);
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
                .orElseThrow(() -> new AvaliacaoNaoEncontradaException("Avaliação nao encontrada"));

        UsuarioDTO usuario = autenticacaoService.getUsuarioAtual();

        if (!temPermissaoParaVisualizar(avaliacao, usuario.uuid(), usuario.perfis().iterator().next())) {
            throw new UsuarioSemAutorizacaoException("Usuario nao autorizado a ver tratamentos desta avaliacao");
        }

        return agendamentoRepository.findByAvaliacaoId(avaliacaoId)
                .stream()
                .map(agendamentoUtil::convertToViewDTO)
                .collect(Collectors.toList());
    }
}