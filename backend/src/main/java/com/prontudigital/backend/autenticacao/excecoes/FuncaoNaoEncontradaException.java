package com.prontudigital.backend.autenticacao.excecoes;

public class FuncaoNaoEncontradaException extends RuntimeException {
    public FuncaoNaoEncontradaException(String role) {
        super("Função não encontrada : " + role);
    }
}
