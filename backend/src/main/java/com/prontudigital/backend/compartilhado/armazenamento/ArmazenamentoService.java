package com.prontudigital.backend.compartilhado.armazenamento;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Abstracao de armazenamento de arquivos binarios.
 *
 * O dominio (ex.: anexos do prontuario - RF15) depende apenas desta interface,
 * nunca de detalhes de infraestrutura. A implementacao inicial grava em
 * filesystem/volume; trocar por S3/MinIO no futuro nao exige mexer no dominio.
 *
 * A {@code chave} e um identificador logico e estavel do arquivo (ex.:
 * {@code pacienteUuid/uuid.pdf}); cabe a implementacao mapea-la para um caminho
 * fisico ou chave de objeto.
 */
public interface ArmazenamentoService {

    /** Persiste o conteudo sob a chave informada, sobrescrevendo se ja existir. */
    void salvar(String chave, InputStream conteudo, long tamanho);

    /** Recupera o conteudo previamente armazenado como {@link Resource} para streaming. */
    Resource carregar(String chave);

    /** Remove o arquivo. Nao falha se a chave nao existir. */
    void remover(String chave);
}
