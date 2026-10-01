package com.prontudigital.backend.compartilhado.excecoes;

public abstract class ExcecaoBase extends RuntimeException {
    protected ExcecaoBase(String mensagem) {
        super(mensagem);
    }
}