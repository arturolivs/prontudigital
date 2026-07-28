package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoException;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoProperties;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoService;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.prontuario.dto.AnexoDownloadDTO;
import com.prontudigital.backend.prontuario.dto.AnexoResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Anexo;
import com.prontudigital.backend.prontuario.excecoes.AnexoInvalidoException;
import com.prontudigital.backend.prontuario.excecoes.AnexoNaoEncontradoException;
import com.prontudigital.backend.prontuario.repositorios.AnexoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import com.prontudigital.backend.prontuario.servicos.AnexoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnexoServiceImpl implements AnexoService {

    private final AnexoRepository anexoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final ProntuarioPermissaoPolicy permissaoPolicy;
    private final ArmazenamentoService armazenamentoService;
    private final ArmazenamentoProperties propriedades;

    @Override
    @Transactional(readOnly = true)
    public List<AnexoResponseDTO> listarPorPaciente(UUID pacienteUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(Mensagens.get("anexo.nao-autorizado.visualizar"));
        }

        String pacienteNome = usuarioService.buscarPorUuid(pacienteUuid).nomeCompleto();
        return anexoRepository.findByPacienteUuidOrderByCriadoEmDesc(pacienteUuid).stream()
                .map(a -> toResponse(a, pacienteNome))
                .toList();
    }

    @Override
    @Transactional
    public AnexoResponseDTO enviar(UUID pacienteUuid, MultipartFile arquivo, UUID agendamentoUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        usuarioService.validarUsuarioExiste(pacienteUuid);

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(Mensagens.get("anexo.nao-autorizado.enviar"));
        }

        validarArquivo(arquivo);

        String nomeOriginal = sanitizarNome(arquivo.getOriginalFilename());
        String chave = pacienteUuid + "/" + UUID.randomUUID() + extensaoPara(arquivo.getContentType());

        try (InputStream conteudo = arquivo.getInputStream()) {
            armazenamentoService.salvar(chave, conteudo, arquivo.getSize());
        } catch (IOException e) {
            log.error("Falha ao ler upload do paciente {}: {}", pacienteUuid, e.getMessage());
            throw new ArmazenamentoException(Mensagens.get("anexo.armazenamento.falha-gravar"));
        }

        Anexo anexo = Anexo.builder()
                .pacienteUuid(pacienteUuid)
                .agendamentoUuid(agendamentoUuid)
                .nomeOriginal(nomeOriginal)
                .tipoConteudo(arquivo.getContentType())
                .tamanhoBytes(arquivo.getSize())
                .chaveArmazenamento(chave)
                .registradoPor(usuario.uuid())
                .build();

        Anexo salvo = anexoRepository.save(anexo);
        log.info("Anexo {} ({} bytes) enviado para paciente {} por {}",
                salvo.getUuid(), salvo.getTamanhoBytes(), pacienteUuid, usuario.uuid());
        return toResponse(salvo, usuarioService.buscarPorUuid(pacienteUuid).nomeCompleto());
    }

    @Override
    @Transactional(readOnly = true)
    public AnexoDownloadDTO baixar(UUID pacienteUuid, UUID anexoUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(Mensagens.get("anexo.nao-autorizado.visualizar"));
        }

        Anexo anexo = buscarObrigatorio(anexoUuid, pacienteUuid);
        Resource recurso = armazenamentoService.carregar(anexo.getChaveArmazenamento());
        return new AnexoDownloadDTO(recurso, anexo.getNomeOriginal(),
                anexo.getTipoConteudo(), anexo.getTamanhoBytes());
    }

    @Override
    @Transactional
    public void excluir(UUID pacienteUuid, UUID anexoUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(Mensagens.get("anexo.nao-autorizado.excluir"));
        }

        Anexo anexo = buscarObrigatorio(anexoUuid, pacienteUuid);
        armazenamentoService.remover(anexo.getChaveArmazenamento());
        anexoRepository.delete(anexo);
        log.info("Anexo {} excluido do paciente {} por {}", anexoUuid, pacienteUuid, usuario.uuid());
    }

    private Anexo buscarObrigatorio(UUID anexoUuid, UUID pacienteUuid) {
        return anexoRepository.findByUuidAndPacienteUuid(anexoUuid, pacienteUuid)
                .orElseThrow(() -> new AnexoNaoEncontradoException(
                        Mensagens.get("anexo.nao-encontrado")));
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new AnexoInvalidoException(Mensagens.get("anexo.arquivo-vazio"));
        }
        if (arquivo.getSize() > propriedades.getTamanhoMaximoBytes()) {
            long maxMb = propriedades.getTamanhoMaximoBytes() / (1024 * 1024);
            throw new AnexoInvalidoException(Mensagens.get("anexo.tamanho-excedido", maxMb));
        }
        String tipo = arquivo.getContentType();
        if (tipo == null || !propriedades.getTiposPermitidos().contains(tipo)) {
            throw new AnexoInvalidoException(Mensagens.get("anexo.tipo-nao-permitido",
                    String.join(", ", propriedades.getTiposPermitidos())));
        }
    }

    /**
     * Remove diretorios do nome original (mantendo apenas o basename) e caracteres
     * de controle, evitando path traversal e injecao no cabecalho Content-Disposition.
     */
    private String sanitizarNome(String original) {
        if (original == null || original.isBlank()) {
            return "arquivo";
        }
        String limpo = original.replaceAll("[\\r\\n\"]", "").trim();
        int corte = Math.max(limpo.lastIndexOf('/'), limpo.lastIndexOf('\\'));
        if (corte >= 0) {
            limpo = limpo.substring(corte + 1);
        }
        limpo = limpo.replace("..", "").trim();
        if (limpo.isBlank()) {
            return "arquivo";
        }
        return limpo.length() > 255 ? limpo.substring(limpo.length() - 255) : limpo;
    }

    /** Extensao derivada do tipo MIME (ja validado), evitando confiar no nome enviado. */
    private String extensaoPara(String tipoConteudo) {
        return switch (tipoConteudo) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }

    private AnexoResponseDTO toResponse(Anexo a, String pacienteNome) {
        return new AnexoResponseDTO(
                a.getUuid(),
                a.getPacienteUuid(),
                pacienteNome,
                a.getAgendamentoUuid(),
                a.getNomeOriginal(),
                a.getTipoConteudo(),
                a.getTamanhoBytes(),
                a.getRegistradoPor(),
                a.getCriadoEm());
    }
}
