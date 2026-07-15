package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.ProcedimentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.ProcedimentoResponseDTO;
import com.prontudigital.backend.agendamento.entidades.Procedimento;
import com.prontudigital.backend.agendamento.excecoes.ProcedimentoEmUsoException;
import com.prontudigital.backend.agendamento.excecoes.ProcedimentoJaExisteException;
import com.prontudigital.backend.agendamento.excecoes.ProcedimentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.ProcedimentoRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.ProcedimentoService;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcedimentoServiceImpl implements ProcedimentoService {

    private final ProcedimentoRepository procedimentoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<ProcedimentoResponseDTO> listar(boolean incluirInativos) {
        List<Procedimento> procedimentos = incluirInativos
                ? procedimentoRepository.findAllByOrderByNomeAsc()
                : procedimentoRepository.findByAtivoTrueOrderByNomeAsc();
        return procedimentos.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedimentoResponseDTO buscarPorId(Long id) {
        return toResponse(buscarObrigatorio(id));
    }

    @Override
    @Transactional
    public ProcedimentoResponseDTO criar(ProcedimentoRequestDTO request) {
        UsuarioDTO usuario = exigirAdmin();

        String nome = request.nome().trim();
        if (procedimentoRepository.existsByNomeIgnoreCase(nome)) {
            throw new ProcedimentoJaExisteException(
                    Mensagens.get("procedimento.nome-existente", nome));
        }

        Procedimento procedimento = Procedimento.builder()
                .nome(nome)
                .descricao(request.descricao())
                .duracaoPadraoMinutos(request.duracaoPadraoMinutos())
                .ativo(request.ativo() == null || request.ativo())
                .build();

        Procedimento salvo = procedimentoRepository.save(procedimento);
        log.info("Procedimento '{}' criado (id={}) por {}", nome, salvo.getId(), usuario.uuid());
        return toResponse(salvo);
    }

    @Override
    @Transactional
    public ProcedimentoResponseDTO atualizar(Long id, ProcedimentoRequestDTO request) {
        UsuarioDTO usuario = exigirAdmin();

        Procedimento procedimento = buscarObrigatorio(id);
        String nome = request.nome().trim();
        if (procedimentoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ProcedimentoJaExisteException(
                    Mensagens.get("procedimento.nome-existente", nome));
        }

        procedimento.setNome(nome);
        procedimento.setDescricao(request.descricao());
        procedimento.setDuracaoPadraoMinutos(request.duracaoPadraoMinutos());
        if (request.ativo() != null) {
            procedimento.setAtivo(request.ativo());
        }

        Procedimento salvo = procedimentoRepository.save(procedimento);
        log.info("Procedimento {} atualizado por {}", id, usuario.uuid());
        return toResponse(salvo);
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        UsuarioDTO usuario = exigirAdmin();

        Procedimento procedimento = buscarObrigatorio(id);
        if (agendamentoRepository.existsByProcedimentoId(id)) {
            throw new ProcedimentoEmUsoException(Mensagens.get("procedimento.em-uso"));
        }

        procedimentoRepository.delete(procedimento);
        log.info("Procedimento {} excluido por {}", id, usuario.uuid());
    }

    private UsuarioDTO exigirAdmin() {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!"ADMIN".equals(permissaoPolicy.perfilEfetivo(usuario))) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("procedimento.nao-autorizado.gerenciar"));
        }
        return usuario;
    }

    private Procedimento buscarObrigatorio(Long id) {
        return procedimentoRepository.findById(id)
                .orElseThrow(() -> new ProcedimentoNaoEncontradoException(
                        Mensagens.get("procedimento.nao-encontrado", id)));
    }

    private ProcedimentoResponseDTO toResponse(Procedimento p) {
        return new ProcedimentoResponseDTO(
                p.getId(),
                p.getCodigo(),
                p.getNome(),
                p.getDescricao(),
                p.getDuracaoPadraoMinutos(),
                p.getAtivo(),
                p.getCriadoEm(),
                p.getAtualizadoEm());
    }
}
