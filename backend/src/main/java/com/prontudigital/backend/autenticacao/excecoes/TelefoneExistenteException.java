package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

public class TelefoneExistenteException extends ExcecaoBase {
    public TelefoneExistenteException(String telefone) {
        super(Mensagens.get("usuario.telefone-existente", telefone));
    }
}
