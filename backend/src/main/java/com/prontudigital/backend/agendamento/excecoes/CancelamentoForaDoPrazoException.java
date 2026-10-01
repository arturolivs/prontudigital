package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class CancelamentoForaDoPrazoException extends ExcecaoBase {
    public CancelamentoForaDoPrazoException(String message) {
        super(message);
    }
}
