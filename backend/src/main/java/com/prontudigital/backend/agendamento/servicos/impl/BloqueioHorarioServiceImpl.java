package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import com.prontudigital.backend.agendamento.dto.BloqueioRecorrenteDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.entidades.BloqueioRecorrente;
import com.prontudigital.backend.agendamento.enums.TipoBloqueio;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioRecorrenteRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.BloqueioHorarioService;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BloqueioHorarioServiceImpl implements BloqueioHorarioService {

    private final BloqueioHorarioRepository repository;
    private final BloqueioRecorrenteRepository recorrenteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional
    public BloqueioHorarioDTO criar(BloqueioHorarioDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PROFISSIONAL".equals(perfil) &&
                !request.profissionalUuid().equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("bloqueio.nao-autorizado.propria-agenda"));
        }
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("bloqueio.paciente-nao-cria"));
        }

        if (!request.fimEm().isAfter(request.inicioEm())) {
            throw new AgendamentoInvalidoException(Mensagens.get("bloqueio.fim-antes-inicio"));
        }

        BloqueioHorario bloqueio = BloqueioHorario.builder()
                .profissionalUuid(request.profissionalUuid())
                .inicioEm(request.inicioEm())
                .fimEm(request.fimEm())
                .motivo(request.motivo())
                .tipo(request.tipo())
                .build();

        return toBloqueioDTO(repository.save(bloqueio));
    }

    @Override
    @Transactional
    public void remover(Long id) {
        BloqueioHorario bloqueio = repository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("bloqueio.nao-encontrado", id)));

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PROFISSIONAL".equals(perfil) &&
                !bloqueio.getProfissionalUuid().equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("bloqueio.sem-autorizacao-remover"));
        }

        repository.delete(bloqueio);
    }

    @Override
    public List<BloqueioHorarioDTO> listarPorProfissional(UUID profissionalUuid,
                                                          LocalDate inicio,
                                                          LocalDate fim) {
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime fimDt = fim.atTime(LocalTime.MAX);

        List<BloqueioHorarioDTO> resultado = new ArrayList<>(
                repository.findConflitos(profissionalUuid, inicioDt, fimDt)
                        .stream()
                        .map(this::toBloqueioDTO)
                        .toList()
        );

        agendamentoRepository
                .findOcupadosPorProfissional(profissionalUuid, inicioDt, fimDt)
                .stream()
                .map(this::agendamentoParaBloqueioDTO)
                .forEach(resultado::add);

        recorrenteRepository.findByProfissionalUuidAndAtivoTrue(profissionalUuid)
                .forEach(regra -> expandirRecorrente(regra, inicio, fim, resultado));

        return resultado;
    }

    private void expandirRecorrente(BloqueioRecorrente regra,
                                    LocalDate inicio, LocalDate fim,
                                    List<BloqueioHorarioDTO> destino) {
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            if (cursor.getDayOfWeek().getValue() == regra.getDiaSemana()) {
                destino.add(new BloqueioHorarioDTO(
                        null,
                        regra.getUuid(),
                        regra.getProfissionalUuid(),
                        cursor.atTime(regra.getHoraInicio()),
                        cursor.atTime(regra.getHoraFim()),
                        regra.getMotivo(),
                        regra.getTipo(),
                        regra.getUuid()
                ));
            }
            cursor = cursor.plusDays(1);
        }
    }

    // ── Recorrentes ──────────────────────────────────────────────

    @Override
    @Transactional
    public BloqueioRecorrenteDTO criarRecorrente(BloqueioRecorrenteDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PROFISSIONAL".equals(perfil) &&
                !request.profissionalUuid().equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("bloqueio.nao-autorizado.propria-agenda"));
        }
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("bloqueio.paciente-nao-cria"));
        }

        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new AgendamentoInvalidoException(Mensagens.get("bloqueio.horario-fim-antes-inicio"));
        }

        BloqueioRecorrente regra = BloqueioRecorrente.builder()
                .profissionalUuid(request.profissionalUuid())
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .motivo(request.motivo())
                .tipo(request.tipo())
                .build();

        return toRecorrenteDTO(recorrenteRepository.save(regra));
    }

    @Override
    @Transactional
    public void removerRecorrente(Long id) {
        BloqueioRecorrente regra = recorrenteRepository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("bloqueio.regra-nao-encontrada", id)));

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PROFISSIONAL".equals(perfil) &&
                !regra.getProfissionalUuid().equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("bloqueio.sem-autorizacao-remover-regra"));
        }

        recorrenteRepository.delete(regra);
    }

    @Override
    public List<BloqueioRecorrenteDTO> listarRecorrentesPorProfissional(UUID profissionalUuid) {
        return recorrenteRepository
                .findByProfissionalUuidAndAtivoTrue(profissionalUuid)
                .stream()
                .map(this::toRecorrenteDTO)
                .toList();
    }

    // ── mappers ──────────────────────────────────────────────────

    private BloqueioHorarioDTO toBloqueioDTO(BloqueioHorario b) {
        return new BloqueioHorarioDTO(
                b.getId(), b.getUuid(), b.getProfissionalUuid(),
                b.getInicioEm(), b.getFimEm(), b.getMotivo(), b.getTipo(), null);
    }

    private BloqueioHorarioDTO agendamentoParaBloqueioDTO(Agendamento a) {
        return new BloqueioHorarioDTO(
                null, a.getUuid(), a.getProfissionalUuid(),
                a.getInicioEm(), a.getFimEm(), Mensagens.get("bloqueio.horario-reservado"), TipoBloqueio.INDISPONIVEL, null);
    }

    private BloqueioRecorrenteDTO toRecorrenteDTO(BloqueioRecorrente r) {
        return new BloqueioRecorrenteDTO(
                r.getId(), r.getUuid(), r.getProfissionalUuid(),
                r.getDiaSemana(), r.getHoraInicio(), r.getHoraFim(),
                r.getMotivo(), r.getTipo());
    }
}
