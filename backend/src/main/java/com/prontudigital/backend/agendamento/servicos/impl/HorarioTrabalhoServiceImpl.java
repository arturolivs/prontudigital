package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.HorarioTrabalhoDTO;
import com.prontudigital.backend.agendamento.entidades.HorarioTrabalho;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.BloqueioRecorrenteRepository;
import com.prontudigital.backend.agendamento.repositorios.HorarioTrabalhoRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.HorarioTrabalhoService;
import com.prontudigital.backend.agendamento.utils.ExpedienteEfetivo;
import com.prontudigital.backend.agendamento.utils.ExpedienteEfetivo.Janela;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * RF05 — expediente semanal do profissional.
 *
 * <p>Mesma politica dos bloqueios: o PROFISSIONAL so mexe na propria agenda,
 * o ADMIN mexe em qualquer uma e o PACIENTE nao mexe em nenhuma.
 */
@Service
@RequiredArgsConstructor
public class HorarioTrabalhoServiceImpl implements HorarioTrabalhoService {

    private final HorarioTrabalhoRepository repository;
    private final BloqueioRecorrenteRepository bloqueioRecorrenteRepository;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional
    public HorarioTrabalhoDTO criar(HorarioTrabalhoDTO request) {
        validarAutorizacao(request.profissionalUuid());

        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new AgendamentoInvalidoException(
                    Mensagens.get("horario-trabalho.fim-antes-inicio"));
        }

        boolean sobrepoe = repository
                .findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                        request.profissionalUuid(), request.diaSemana())
                .stream()
                .anyMatch(h -> request.horaInicio().isBefore(h.getHoraFim())
                        && h.getHoraInicio().isBefore(request.horaFim()));

        if (sobrepoe) {
            throw new AgendamentoInvalidoException(
                    Mensagens.get("horario-trabalho.sobreposto"));
        }

        validarNaoAnuladoPorBloqueio(request);

        HorarioTrabalho horario = HorarioTrabalho.builder()
                .profissionalUuid(request.profissionalUuid())
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .ativo(request.ativo() == null || request.ativo())
                .build();

        return toDTO(repository.save(horario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioTrabalhoDTO> listarPorProfissional(UUID profissionalUuid) {
        return repository.findByProfissionalUuidAndAtivoTrue(profissionalUuid)
                .stream()
                .sorted((a, b) -> {
                    int porDia = a.getDiaSemana().compareTo(b.getDiaSemana());
                    return porDia != 0 ? porDia : a.getHoraInicio().compareTo(b.getHoraInicio());
                })
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void remover(Long id) {
        HorarioTrabalho horario = repository.findById(id)
                .orElseThrow(() -> new AgendamentoNaoEncontradoException(
                        Mensagens.get("horario-trabalho.nao-encontrado", id)));

        validarAutorizacao(horario.getProfissionalUuid());

        repository.delete(horario);
    }

    /**
     * Recusa a janela que ja nasce inteiramente coberta pelos bloqueios
     * recorrentes daquele dia da semana.
     *
     * <p>Sem isso a janela e gravada e aparece na tela como expediente, mas
     * nenhum agendamento cabe nela: {@code AgendamentoServiceImpl} aplica
     * expediente e bloqueio em AND, e o bloqueio vence. Cobertura parcial
     * (intervalo de almoco, por exemplo) continua valendo.
     */
    private void validarNaoAnuladoPorBloqueio(HorarioTrabalhoDTO request) {
        List<Janela> bloqueios = bloqueioRecorrenteRepository
                .findByProfissionalUuidAndAtivoTrue(request.profissionalUuid())
                .stream()
                .filter(regra -> request.diaSemana().equals(regra.getDiaSemana()))
                .map(regra -> new Janela(regra.getHoraInicio(), regra.getHoraFim()))
                .toList();

        if (bloqueios.isEmpty()) {
            return;
        }

        List<Janela> nova = List.of(new Janela(request.horaInicio(), request.horaFim()));
        if (ExpedienteEfetivo.cobreTudo(nova, bloqueios)) {
            throw new AgendamentoInvalidoException(
                    Mensagens.get("horario-trabalho.anulado-por-bloqueio"));
        }
    }

    private void validarAutorizacao(UUID profissionalUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        if ("PROFISSIONAL".equals(perfil) && !profissionalUuid.equals(usuario.uuid())) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("horario-trabalho.nao-autorizado.propria-agenda"));
        }
        if ("PACIENTE".equals(perfil)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("horario-trabalho.paciente-nao-gerencia"));
        }
    }

    private HorarioTrabalhoDTO toDTO(HorarioTrabalho h) {
        return new HorarioTrabalhoDTO(
                h.getId(),
                h.getUuid(),
                h.getProfissionalUuid(),
                h.getDiaSemana(),
                h.getHoraInicio(),
                h.getHoraFim(),
                h.getAtivo());
    }
}
