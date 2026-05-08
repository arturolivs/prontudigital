package com.prontudigital.backend.agendamento.eventos;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import java.time.LocalDateTime;

public record AgendamentoReagendadoEvento(
        Agendamento agendamento,
        LocalDateTime inicioAnterior,
        LocalDateTime fimAnterior
) {}