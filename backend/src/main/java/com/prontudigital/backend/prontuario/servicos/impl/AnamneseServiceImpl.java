package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.prontuario.dto.AnamneseRequestDTO;
import com.prontudigital.backend.prontuario.dto.AnamneseResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Anamnese;
import com.prontudigital.backend.prontuario.excecoes.AnamneseJaExisteException;
import com.prontudigital.backend.prontuario.excecoes.AnamneseNaoEncontradaException;
import com.prontudigital.backend.prontuario.repositorios.AnamneseRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import com.prontudigital.backend.prontuario.servicos.AnamneseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnamneseServiceImpl implements AnamneseService {

    private final AnamneseRepository anamneseRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final ProntuarioPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional(readOnly = true)
    public AnamneseResponseDTO buscarPorPaciente(UUID pacienteUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    "Usuario nao autorizado a visualizar o prontuario deste paciente");
        }

        Anamnese anamnese = anamneseRepository.findByPacienteUuid(pacienteUuid)
                .orElseThrow(() -> new AnamneseNaoEncontradaException(
                        "Anamnese nao encontrada para o paciente informado"));

        return toResponse(anamnese);
    }

    @Override
    @Transactional
    public AnamneseResponseDTO registrar(UUID pacienteUuid, AnamneseRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        usuarioService.validarUsuarioExiste(pacienteUuid);

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    "Usuario nao autorizado a registrar o prontuario deste paciente");
        }

        if (anamneseRepository.existsByPacienteUuid(pacienteUuid)) {
            throw new AnamneseJaExisteException(
                    "Paciente ja possui anamnese registrada; utilize a atualizacao");
        }

        Anamnese anamnese = Anamnese.builder()
                .pacienteUuid(pacienteUuid)
                .registradoPor(usuario.uuid())
                .build();
        aplicar(anamnese, request);

        Anamnese salva = anamneseRepository.save(anamnese);
        log.info("Anamnese registrada para paciente {} por {}", pacienteUuid, usuario.uuid());
        return toResponse(salva);
    }

    @Override
    @Transactional
    public AnamneseResponseDTO atualizar(UUID pacienteUuid, AnamneseRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    "Usuario nao autorizado a alterar o prontuario deste paciente");
        }

        Anamnese anamnese = anamneseRepository.findByPacienteUuid(pacienteUuid)
                .orElseThrow(() -> new AnamneseNaoEncontradaException(
                        "Anamnese nao encontrada para o paciente informado"));

        aplicar(anamnese, request);
        anamnese.setRegistradoPor(usuario.uuid());

        Anamnese salva = anamneseRepository.save(anamnese);
        log.info("Anamnese atualizada para paciente {} por {}", pacienteUuid, usuario.uuid());
        return toResponse(salva);
    }

    private void aplicar(Anamnese anamnese, AnamneseRequestDTO request) {
        anamnese.setQueixaPrincipal(request.queixaPrincipal());
        anamnese.setHistoricoDoencaAtual(request.historicoDoencaAtual());
        anamnese.setHistoricoMedicoPregresso(request.historicoMedicoPregresso());
        anamnese.setAlergias(request.alergias());
        anamnese.setMedicamentosEmUso(request.medicamentosEmUso());
        anamnese.setHistoricoFamiliar(request.historicoFamiliar());
        anamnese.setHabitos(request.habitos());
        anamnese.setObservacoes(request.observacoes());
    }

    private AnamneseResponseDTO toResponse(Anamnese a) {
        String pacienteNome = usuarioService.buscarPorUuid(a.getPacienteUuid()).nomeCompleto();
        return new AnamneseResponseDTO(
                a.getUuid(),
                a.getPacienteUuid(),
                pacienteNome,
                a.getQueixaPrincipal(),
                a.getHistoricoDoencaAtual(),
                a.getHistoricoMedicoPregresso(),
                a.getAlergias(),
                a.getMedicamentosEmUso(),
                a.getHistoricoFamiliar(),
                a.getHabitos(),
                a.getObservacoes(),
                a.getRegistradoPor(),
                a.getCriadoEm(),
                a.getAtualizadoEm());
    }
}
