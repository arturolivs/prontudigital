package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.BloqueioHorarioService;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BloqueioHorarioServiceImpl implements BloqueioHorarioService {

    private final BloqueioHorarioRepository repository;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional
    public BloqueioHorarioDTO criar(BloqueioHorarioDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        // PROFISSIONAL só pode bloquear o próprio horário
        if ("PROFISSIONAL".equals(perfil) &&
                !request.profissionalUuid().equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    "Profissional so pode bloquear sua propria agenda");
        }
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    "Paciente nao pode criar bloqueios de horario");
        }

        if (!request.fimEm().isAfter(request.inicioEm())) {
            throw new AgendamentoInvalidoException("Fim deve ser posterior ao inicio");
        }

        BloqueioHorario bloqueio = BloqueioHorario.builder()
                .profissionalUuid(request.profissionalUuid())
                .inicioEm(request.inicioEm())
                .fimEm(request.fimEm())
                .motivo(request.motivo())
                .tipo(request.tipo())
                .build();

        return toDTO(repository.save(bloqueio));
    }

    @Override
    @Transactional
    public void remover(Long id) {
        BloqueioHorario bloqueio = repository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        "Bloqueio nao encontrado: " + id));

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PROFISSIONAL".equals(perfil) &&
                !bloqueio.getProfissionalUuid().equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    "Sem autorizacao para remover este bloqueio");
        }

        repository.delete(bloqueio);
    }

    @Override
    public List<BloqueioHorarioDTO> listarPorProfissional(UUID profissionalUuid,
                                                          LocalDate inicio,
                                                          LocalDate fim) {
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime fimDt = fim.atTime(LocalTime.MAX);

        return repository.findConflitos(profissionalUuid, inicioDt, fimDt)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private BloqueioHorarioDTO toDTO(BloqueioHorario b) {
        return new BloqueioHorarioDTO(
                b.getId(), b.getUuid(), b.getProfissionalUuid(),
                b.getInicioEm(), b.getFimEm(), b.getMotivo(), b.getTipo());
    }
}