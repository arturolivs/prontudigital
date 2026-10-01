package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class HorarioIndisponivelException extends ExcecaoBase {
    public HorarioIndisponivelException(String message) {
        super(message);
    }
}
