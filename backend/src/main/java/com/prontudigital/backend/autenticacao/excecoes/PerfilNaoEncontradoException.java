package com.prontudigital.backend.autenticacao.excecoes;

public class PerfilNaoEncontradoException extends RuntimeException {
    public PerfilNaoEncontradoException(String perfil) {
        super("Perfil não encontrado : " + perfil);
    }
}
