package com.prontudigital.backend.agendamento.repositorios;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PacienteAgendamentoResumo {
    UUID getPacienteUuid();
    LocalDateTime getUltimoAgendamento();
}
