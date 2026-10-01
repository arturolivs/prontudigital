package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.prontuario.dto.PrescricaoRequestDTO;
import com.prontudigital.backend.prontuario.dto.PrescricaoResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Prescricao;
import com.prontudigital.backend.prontuario.excecoes.PrescricaoNaoEncontradaException;
import com.prontudigital.backend.prontuario.repositorios.PrescricaoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import com.prontudigital.backend.prontuario.servicos.PrescricaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescricaoServiceImpl implements PrescricaoService {

    private final PrescricaoRepository prescricaoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final ProntuarioPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<PrescricaoResponseDTO> listarPorPaciente(UUID pacienteUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("prescricao.nao-autorizado.visualizar"));
        }

        String pacienteNome = usuarioService.buscarPorUuid(pacienteUuid).nomeCompleto();
        return prescricaoRepository.findByPacienteUuidOrderByCriadoEmDesc(pacienteUuid).stream()
                .map(p -> toResponse(p, pacienteNome))
                .toList();
    }

    @Override
    @Transactional
    public PrescricaoResponseDTO registrar(UUID pacienteUuid, PrescricaoRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        usuarioService.validarUsuarioExiste(pacienteUuid);

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("prescricao.nao-autorizado.registrar"));
        }

        Prescricao prescricao = Prescricao.builder()
                .pacienteUuid(pacienteUuid)
                .registradoPor(usuario.uuid())
                .build();
        aplicar(prescricao, request);

        Prescricao salva = prescricaoRepository.save(prescricao);
        log.info("Prescricao registrada para paciente {} por {}", pacienteUuid, usuario.uuid());
        return toResponse(salva, usuarioService.buscarPorUuid(pacienteUuid).nomeCompleto());
    }

    @Override
    @Transactional
    public PrescricaoResponseDTO atualizar(UUID pacienteUuid, UUID prescricaoUuid, PrescricaoRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("prescricao.nao-autorizado.alterar"));
        }

        Prescricao prescricao = buscarObrigatoria(prescricaoUuid, pacienteUuid);
        aplicar(prescricao, request);
        prescricao.setRegistradoPor(usuario.uuid());

        Prescricao salva = prescricaoRepository.save(prescricao);
        log.info("Prescricao {} atualizada para paciente {} por {}",
                prescricaoUuid, pacienteUuid, usuario.uuid());
        return toResponse(salva, usuarioService.buscarPorUuid(pacienteUuid).nomeCompleto());
    }

    @Override
    @Transactional
    public void excluir(UUID pacienteUuid, UUID prescricaoUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("prescricao.nao-autorizado.excluir"));
        }

        Prescricao prescricao = buscarObrigatoria(prescricaoUuid, pacienteUuid);
        prescricaoRepository.delete(prescricao);
        log.info("Prescricao {} excluida do paciente {} por {}",
                prescricaoUuid, pacienteUuid, usuario.uuid());
    }

    private Prescricao buscarObrigatoria(UUID prescricaoUuid, UUID pacienteUuid) {
        return prescricaoRepository.findByUuidAndPacienteUuid(prescricaoUuid, pacienteUuid)
                .orElseThrow(() -> new PrescricaoNaoEncontradaException(
                        Mensagens.get("prescricao.nao-encontrada")));
    }

    private void aplicar(Prescricao prescricao, PrescricaoRequestDTO request) {
        prescricao.setTipo(request.tipo());
        prescricao.setDescricao(request.descricao());
        prescricao.setPosologia(request.posologia());
        prescricao.setFrequencia(request.frequencia());
        prescricao.setDuracao(request.duracao());
        prescricao.setOrientacoes(request.orientacoes());
        prescricao.setAgendamentoUuid(request.agendamentoUuid());
    }

    private PrescricaoResponseDTO toResponse(Prescricao p, String pacienteNome) {
        return new PrescricaoResponseDTO(
                p.getUuid(),
                p.getPacienteUuid(),
                pacienteNome,
                p.getAgendamentoUuid(),
                p.getTipo(),
                p.getDescricao(),
                p.getPosologia(),
                p.getFrequencia(),
                p.getDuracao(),
                p.getOrientacoes(),
                p.getRegistradoPor(),
                p.getCriadoEm(),
                p.getAtualizadoEm());
    }
}
