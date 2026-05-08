package com.prontudigital.backend.compartilhado.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL) // omite campos nulos no JSON
public record ErroPadraoDTO(
        int status,
        String erro,
        String mensagem,
        String caminho,
        LocalDateTime timestamp,
        List<String> detalhes
) {
    public ErroPadraoDTO(int status, String erro, String mensagem, String caminho) {
        this(status, erro, mensagem, caminho, LocalDateTime.now(), null);
    }

    public ErroPadraoDTO(int status, String erro, String mensagem,
                         String caminho, List<String> detalhes) {
        this(status, erro, mensagem, caminho, LocalDateTime.now(), detalhes);
    }
}