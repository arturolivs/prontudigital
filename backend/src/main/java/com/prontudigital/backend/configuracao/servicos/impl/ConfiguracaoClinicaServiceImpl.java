package com.prontudigital.backend.configuracao.servicos.impl;

import com.prontudigital.backend.autenticacao.util.UsuarioUtil;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoException;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoService;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.configuracao.dto.ConfiguracaoClinicaRequestDTO;
import com.prontudigital.backend.configuracao.dto.ConfiguracaoClinicaResponseDTO;
import com.prontudigital.backend.configuracao.dto.LogoDownloadDTO;
import com.prontudigital.backend.configuracao.entidades.ConfiguracaoClinica;
import com.prontudigital.backend.configuracao.excecoes.LogoInvalidaException;
import com.prontudigital.backend.configuracao.repositorios.ConfiguracaoClinicaRepository;
import com.prontudigital.backend.configuracao.servicos.ConfiguracaoClinicaService;
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
public class ConfiguracaoClinicaServiceImpl implements ConfiguracaoClinicaService {

    /**
     * Prefixo da chave no armazenamento. Separa a logo dos anexos de paciente,
     * que usam {@code pacienteUuid/...} — assim um backup seletivo consegue
     * distinguir dado clinico de material de marca.
     */
    private static final String PREFIXO_CHAVE = "configuracao/";

    /**
     * Formatos aceitos para a logo. Nao inclui PDF (que o upload de anexo
     * aceita): logo e imagem, e o PdfBuilder precisa embutir um raster.
     */
    private static final List<String> TIPOS_LOGO = List.of(
            "image/jpeg", "image/png", "image/webp");

    /** 2 MB — logo e elemento de cabecalho, nao precisa de mais. */
    private static final long TAMANHO_MAXIMO_LOGO = 2L * 1024 * 1024;

    private final ConfiguracaoClinicaRepository repository;
    private final ArmazenamentoService armazenamentoService;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracaoClinicaResponseDTO buscar() {
        return paraDTO(obterConfiguracao());
    }

    @Override
    @Transactional
    public ConfiguracaoClinicaResponseDTO atualizar(ConfiguracaoClinicaRequestDTO request) {
        ConfiguracaoClinica config = obterConfiguracao();

        config.setNome(request.nome().trim());
        config.setCnpj(UsuarioUtil.somenteDigitos(request.cnpj()));
        config.setTelefone(UsuarioUtil.normalizar(request.telefone()));
        config.setEmail(UsuarioUtil.normalizar(request.email()));
        config.setSite(UsuarioUtil.normalizar(request.site()));
        config.setRodapeDocumentos(UsuarioUtil.normalizar(request.rodapeDocumentos()));

        // Endereco ausente preserva o gravado, mesma regra do perfil de usuario.
        var novoEndereco = UsuarioUtil.converterEnderecoDeDTO(request.endereco());
        if (novoEndereco != null) {
            config.setEndereco(novoEndereco);
        }

        log.info("Configuracao da clinica atualizada: nome={}", config.getNome());
        return paraDTO(repository.save(config));
    }

    @Override
    @Transactional
    public ConfiguracaoClinicaResponseDTO enviarLogo(MultipartFile arquivo) {
        validarLogo(arquivo);

        ConfiguracaoClinica config = obterConfiguracao();
        String chaveAnterior = config.getLogoChave();

        String chave = PREFIXO_CHAVE + UUID.randomUUID() + extensaoPara(arquivo.getContentType());

        try (InputStream conteudo = arquivo.getInputStream()) {
            armazenamentoService.salvar(chave, conteudo, arquivo.getSize());
        } catch (IOException e) {
            log.error("Falha ao ler upload da logo: {}", e.getMessage());
            throw new ArmazenamentoException(Mensagens.get("anexo.armazenamento.falha-gravar"));
        }

        config.setLogoChave(chave);
        config.setLogoTipoConteudo(arquivo.getContentType());
        ConfiguracaoClinica salva = repository.save(config);

        // Só remove a antiga depois que a nova está gravada e referenciada —
        // na ordem inversa, uma falha deixaria a clinica sem logo nenhuma.
        removerArquivoIgnorandoFalha(chaveAnterior);

        log.info("Logo da clinica atualizada: chave={}", chave);
        return paraDTO(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public LogoDownloadDTO baixarLogo() {
        ConfiguracaoClinica config = obterConfiguracao();
        if (!config.temLogo()) {
            throw new LogoInvalidaException(Mensagens.get("configuracao.logo.nao-cadastrada"));
        }
        Resource recurso = armazenamentoService.carregar(config.getLogoChave());
        return new LogoDownloadDTO(recurso, config.getLogoTipoConteudo());
    }

    @Override
    @Transactional
    public ConfiguracaoClinicaResponseDTO removerLogo() {
        ConfiguracaoClinica config = obterConfiguracao();
        String chave = config.getLogoChave();

        config.setLogoChave(null);
        config.setLogoTipoConteudo(null);
        ConfiguracaoClinica salva = repository.save(config);

        removerArquivoIgnorandoFalha(chave);
        log.info("Logo da clinica removida");
        return paraDTO(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracaoClinica obterConfiguracao() {
        // A linha e criada pela migracao V28 e o CHECK impede outras, entao
        // ausencia aqui significa banco adulterado — falha alto em vez de
        // seguir com um objeto vazio que geraria documentos sem cabecalho.
        return repository.findById(ConfiguracaoClinica.ID_UNICO)
                .orElseThrow(() -> new IllegalStateException(
                        Mensagens.get("configuracao.nao-inicializada")));
    }

    // --- Apoio ---------------------------------------------------------

    private void validarLogo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new LogoInvalidaException(Mensagens.get("anexo.arquivo-vazio"));
        }
        if (!TIPOS_LOGO.contains(arquivo.getContentType())) {
            throw new LogoInvalidaException(Mensagens.get("configuracao.logo.tipo-invalido"));
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_LOGO) {
            throw new LogoInvalidaException(Mensagens.get("configuracao.logo.tamanho-excedido", 2));
        }
    }

    /**
     * A remocao do arquivo antigo e higiene, nao regra de negocio: falhar aqui
     * nao pode desfazer a troca de logo que ja foi persistida.
     */
    private void removerArquivoIgnorandoFalha(String chave) {
        if (chave == null || chave.isBlank()) {
            return;
        }
        try {
            armazenamentoService.remover(chave);
        } catch (RuntimeException e) {
            log.warn("Logo anterior {} nao pode ser removida: {}", chave, e.getMessage());
        }
    }

    private String extensaoPara(String tipoConteudo) {
        if (tipoConteudo == null) return "";
        return switch (tipoConteudo) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private ConfiguracaoClinicaResponseDTO paraDTO(ConfiguracaoClinica c) {
        return new ConfiguracaoClinicaResponseDTO(
                c.getNome(),
                c.getCnpj(),
                c.getTelefone(),
                c.getEmail(),
                c.getSite(),
                UsuarioUtil.converterEnderecoParaDTO(c.getEndereco()),
                c.getRodapeDocumentos(),
                c.temLogo(),
                c.getAtualizadoEm());
    }
}
