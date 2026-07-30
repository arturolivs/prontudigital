package com.prontudigital.backend.configuracao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

/** Logo invalida: arquivo vazio, tipo nao permitido, acima do limite ou ausente. */
public class LogoInvalidaException extends ExcecaoBase {
    public LogoInvalidaException(String message) {
        super(message);
    }
}
