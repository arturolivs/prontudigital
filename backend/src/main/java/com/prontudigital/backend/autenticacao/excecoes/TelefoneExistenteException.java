package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class TelefoneExistenteException extends ExcecaoBase {
    public TelefoneExistenteException(String telefone) {
        super("Telefone já cadastrado: " + telefone);
    }
}
