package com.prontudigital.backend.prontuario.dto;

import org.springframework.core.io.Resource;

/**
 * Transporte interno para streaming de download de um anexo: o recurso binario
 * mais os metadados necessarios para montar os cabecalhos HTTP da resposta.
 */
public record AnexoDownloadDTO(
        Resource recurso,
        String nomeOriginal,
        String tipoConteudo,
        long tamanhoBytes
) {}
