package com.prontudigital.backend.compartilhado.armazenamento.impl;

import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoException;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoProperties;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoService;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Implementacao de {@link ArmazenamentoService} baseada em filesystem/volume.
 *
 * Todas as chaves sao resolvidas sob o diretorio raiz configurado e validadas
 * contra path traversal ({@code ../}) antes de qualquer operacao de I/O.
 */
@Service
@Slf4j
public class ArmazenamentoFilesystemService implements ArmazenamentoService {

    private final Path raiz;

    public ArmazenamentoFilesystemService(ArmazenamentoProperties propriedades) {
        this.raiz = Paths.get(propriedades.getDiretorioBase()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
            log.info("Armazenamento de anexos em {}", raiz);
        } catch (IOException e) {
            throw new ArmazenamentoException(Mensagens.get("anexo.armazenamento.falha-inicializacao"));
        }
    }

    @Override
    public void salvar(String chave, InputStream conteudo, long tamanho) {
        Path destino = resolver(chave);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Falha ao gravar anexo em {}: {}", destino, e.getMessage());
            throw new ArmazenamentoException(Mensagens.get("anexo.armazenamento.falha-gravar"));
        }
    }

    @Override
    public Resource carregar(String chave) {
        Path origem = resolver(chave);
        Resource recurso = new FileSystemResource(origem);
        if (!recurso.exists() || !recurso.isReadable()) {
            throw new ArmazenamentoException(Mensagens.get("anexo.armazenamento.falha-ler"));
        }
        return recurso;
    }

    @Override
    public void remover(String chave) {
        Path alvo = resolver(chave);
        try {
            Files.deleteIfExists(alvo);
        } catch (IOException e) {
            log.error("Falha ao remover anexo {}: {}", alvo, e.getMessage());
            throw new ArmazenamentoException(Mensagens.get("anexo.armazenamento.falha-remover"));
        }
    }

    /** Resolve a chave sob a raiz, rejeitando qualquer tentativa de escapar do diretorio base. */
    private Path resolver(String chave) {
        Path destino = raiz.resolve(chave).normalize();
        if (!destino.startsWith(raiz)) {
            throw new ArmazenamentoException(Mensagens.get("anexo.armazenamento.chave-invalida"));
        }
        return destino;
    }
}
