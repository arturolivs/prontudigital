package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class PerfilNaoEncontradoException extends ExcecaoBase {
    public PerfilNaoEncontradoException(String perfil) {
        super("Perfil não encontrado : " + perfil);
    }
}
