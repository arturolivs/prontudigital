package com.prontudigital.backend.prontuario.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

/** Arquivo enviado invalido: vazio, tipo nao permitido ou acima do tamanho maximo. */
public class AnexoInvalidoException extends ExcecaoBase {
    public AnexoInvalidoException(String message) {
        super(message);
    }
}
