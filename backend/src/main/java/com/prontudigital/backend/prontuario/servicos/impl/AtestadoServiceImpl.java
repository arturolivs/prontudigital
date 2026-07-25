package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.compartilhado.documento.FormatoExportacao;
import com.prontudigital.backend.compartilhado.documento.PdfBuilder;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.prontuario.dto.AtestadoRequestDTO;
import com.prontudigital.backend.prontuario.dto.AtestadoResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Atestado;
import com.prontudigital.backend.prontuario.enums.TipoAtestado;
import com.prontudigital.backend.prontuario.excecoes.AtestadoInvalidoException;
import com.prontudigital.backend.prontuario.excecoes.AtestadoNaoEncontradoException;
import com.prontudigital.backend.prontuario.repositorios.AtestadoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import com.prontudigital.backend.prontuario.servicos.AtestadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * RF17 — emissao de atestados.
 *
 * <p>Reusa a {@link ProntuarioPermissaoPolicy} (RN03): o paciente le os proprios
 * atestados mas nunca emite; emitir e do ADMIN e do PROFISSIONAL vinculado.
 */
@Service
@RequiredArgsConstructor
public class AtestadoServiceImpl implements AtestadoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final AtestadoRepository atestadoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final ProntuarioPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<AtestadoResponseDTO> listarPorPaciente(UUID pacienteUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("atestado.nao-autorizado.visualizar"));
        }

        String nomePaciente = nome(pacienteUuid);
        return atestadoRepository.findByPacienteUuidOrderByCriadoEmDesc(pacienteUuid).stream()
                .map(a -> toResponse(a, nomePaciente))
                .toList();
    }

    @Override
    @Transactional
    public AtestadoResponseDTO emitir(UUID pacienteUuid, AtestadoRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        usuarioService.validarUsuarioExiste(pacienteUuid);

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("atestado.nao-autorizado.emitir"));
        }

        validarDias(request);

        Atestado atestado = Atestado.builder()
                .pacienteUuid(pacienteUuid)
                .agendamentoUuid(request.agendamentoUuid())
                .tipo(request.tipo())
                .diasAfastamento(request.tipo() == TipoAtestado.AFASTAMENTO
                        ? request.diasAfastamento() : null)
                .cid(vazioParaNulo(request.cid()))
                .observacoes(vazioParaNulo(request.observacoes()))
                .emitidoPor(usuario.uuid())
                .build();

        return toResponse(atestadoRepository.save(atestado), nome(pacienteUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoGerado gerarPdf(UUID pacienteUuid, UUID atestadoUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("atestado.nao-autorizado.visualizar"));
        }

        Atestado atestado = atestadoRepository
                .findByUuidAndPacienteUuid(atestadoUuid, pacienteUuid)
                .orElseThrow(AtestadoNaoEncontradoException::new);

        return ArquivoGerado.de(
                "atestado_" + atestadoUuid,
                FormatoExportacao.PDF,
                montarPdf(atestado));
    }

    @Override
    @Transactional
    public void remover(UUID pacienteUuid, UUID atestadoUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("atestado.nao-autorizado.emitir"));
        }

        Atestado atestado = atestadoRepository
                .findByUuidAndPacienteUuid(atestadoUuid, pacienteUuid)
                .orElseThrow(AtestadoNaoEncontradoException::new);

        atestadoRepository.delete(atestado);
    }

    // ── regras ───────────────────────────────────────────────────

    /**
     * Dias sao obrigatorios no afastamento e recusados no comparecimento —
     * aceitar dias num atestado de comparecimento produziria um documento que
     * afirma algo que o tipo nao sustenta.
     */
    private void validarDias(AtestadoRequestDTO request) {
        boolean afastamento = request.tipo() == TipoAtestado.AFASTAMENTO;

        if (afastamento && request.diasAfastamento() == null) {
            throw new AtestadoInvalidoException(Mensagens.get("atestado.dias-obrigatorios"));
        }
        if (!afastamento && request.diasAfastamento() != null) {
            throw new AtestadoInvalidoException(Mensagens.get("atestado.dias-nao-aplicaveis"));
        }
    }

    // ── PDF ──────────────────────────────────────────────────────

    private byte[] montarPdf(Atestado atestado) {
        String nomePaciente = nome(atestado.getPacienteUuid());
        UsuarioDTO profissional = atestado.getEmitidoPor() == null
                ? null : buscarUsuario(atestado.getEmitidoPor());

        String titulo = atestado.getTipo() == TipoAtestado.AFASTAMENTO
                ? "Atestado de afastamento"
                : "Atestado de comparecimento";

        PdfBuilder pdf = new PdfBuilder(titulo)
                .subtitulo("Emitido em " + atestado.getCriadoEm().format(DATA_HORA));

        pdf.secao("Paciente");
        pdf.campo("Nome", nomePaciente);

        pdf.secao("Declaração");
        pdf.paragrafo(corpo(atestado, nomePaciente));

        if (atestado.getCid() != null) {
            pdf.campo("CID", atestado.getCid());
        }
        if (atestado.getObservacoes() != null) {
            pdf.secao("Observações");
            pdf.paragrafo(atestado.getObservacoes());
        }

        if (profissional != null) {
            pdf.assinatura(profissional.nomeCompleto(), profissional.coren());
        }

        return pdf.gerar();
    }

    private String corpo(Atestado atestado, String nomePaciente) {
        String data = atestado.getCriadoEm().format(DATA);

        if (atestado.getTipo() == TipoAtestado.AFASTAMENTO) {
            return "Atesto, para os devidos fins, que %s recebeu atendimento nesta clínica "
                    .formatted(nomePaciente)
                    + "em %s e necessita de afastamento de suas atividades por %d dia(s) "
                    .formatted(data, atestado.getDiasAfastamento())
                    + "a contar desta data.";
        }
        return "Atesto, para os devidos fins, que %s compareceu a esta clínica para "
                .formatted(nomePaciente)
                + "atendimento em %s.".formatted(data);
    }

    // ── mapeamento ───────────────────────────────────────────────

    private AtestadoResponseDTO toResponse(Atestado atestado, String nomePaciente) {
        UsuarioDTO profissional = atestado.getEmitidoPor() == null
                ? null : buscarUsuario(atestado.getEmitidoPor());

        return AtestadoResponseDTO.builder()
                .uuid(atestado.getUuid())
                .pacienteUuid(atestado.getPacienteUuid())
                .nomePaciente(nomePaciente)
                .agendamentoUuid(atestado.getAgendamentoUuid())
                .tipo(atestado.getTipo())
                .diasAfastamento(atestado.getDiasAfastamento())
                .cid(atestado.getCid())
                .observacoes(atestado.getObservacoes())
                .emitidoPor(atestado.getEmitidoPor())
                .nomeProfissional(profissional == null ? null : profissional.nomeCompleto())
                .criadoEm(atestado.getCriadoEm())
                .build();
    }

    private String nome(UUID uuid) {
        UsuarioDTO usuario = buscarUsuario(uuid);
        return usuario == null
                ? Mensagens.get("atestado.usuario-indisponivel")
                : usuario.nomeCompleto();
    }

    private UsuarioDTO buscarUsuario(UUID uuid) {
        try {
            return usuarioService.buscarPorUuid(uuid);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
