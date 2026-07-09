package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.FilaEsperaDTO;
import com.prontudigital.backend.agendamento.dto.FilaEsperaRequestDTO;
import com.prontudigital.backend.agendamento.entidades.FilaEspera;
import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import com.prontudigital.backend.agendamento.eventos.AgendamentoCanceladoEvento;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.FilaEsperaRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.FilaEsperaService;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilaEsperaServiceImpl implements FilaEsperaService {

    private final FilaEsperaRepository repository;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional
    public FilaEsperaDTO entrar(FilaEsperaRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PACIENTE".equals(perfil) &&
                !request.pacienteUuid().equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("fila.paciente-so-por-si"));
        }

        FilaEspera fila = FilaEspera.builder()
                .pacienteUuid(request.pacienteUuid())
                .profissionalUuid(request.profissionalUuid())
                .tipoPreferido(request.tipoPreferido())
                .dataPreferida(request.dataPreferida())
                .prioridade(0)
                .status(StatusFilaEspera.ATIVO)
                .build();

        return toDTO(repository.save(fila));
    }

    @Override
    @Transactional
    public void sair(Long id) {
        FilaEspera fila = repository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("fila.entrada-nao-encontrada", id)));

        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        boolean permitido = "ADMIN".equals(perfil) ||
                ("PACIENTE".equals(perfil) && fila.getPacienteUuid().equals(usuario.uuid())) ||
                ("PROFISSIONAL".equals(perfil) && fila.getProfissionalUuid().equals(usuario.uuid()));

        if (!permitido) {
            throw new UsuarioSemAutorizacaoException(Mensagens.get("fila.sem-autorizacao-remover"));
        }

        fila.setStatus(StatusFilaEspera.CANCELADO);
        repository.save(fila);
    }

    @Override
    @Transactional
    public void marcarComoNotificado(Long id) {
        FilaEspera fila = repository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("fila.entrada-nao-encontrada", id)));
        fila.setStatus(StatusFilaEspera.NOTIFICADO);
        repository.save(fila);
    }

    @Override
    public List<FilaEsperaDTO> listarFilaDoProfissional(UUID profissionalUuid) {
        return repository
                .findByProfissionalUuidAndStatusOrderByPrioridadeAscCriadoEmAsc(
                        profissionalUuid, StatusFilaEspera.ATIVO)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvento evento) {
        UUID profissionalUuid = evento.agendamento().getProfissionalUuid();
        List<FilaEspera> fila = repository
                .findByProfissionalUuidAndStatusOrderByPrioridadeAscCriadoEmAsc(
                        profissionalUuid, StatusFilaEspera.ATIVO);

        if (fila.isEmpty()) {
            log.info("Nenhum paciente na fila para profissional={}", profissionalUuid);
            return;
        }

        FilaEspera proximo = fila.get(0);
        log.info("Notificando proximo da fila: paciente={} profissional={}",
                proximo.getPacienteUuid(), profissionalUuid);

        proximo.setStatus(StatusFilaEspera.NOTIFICADO);
        repository.save(proximo);

        // TODO RF09: enviar SMS/email aqui
    }

    private FilaEsperaDTO toDTO(FilaEspera f) {
        return new FilaEsperaDTO(
                f.getId(), f.getUuid(),
                f.getPacienteUuid(), f.getProfissionalUuid(),
                f.getTipoPreferido(), f.getDataPreferida(),
                f.getPrioridade(), f.getStatus(), f.getCriadoEm()
        );
    }
}