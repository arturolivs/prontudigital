package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Paciente agrupado com seus agendamentos no periodo consultado")
public record PacienteAgendamentosDTO(

        UUID pacienteUuid,
        String nomePaciente,
        List<AgendamentoViewDTO> agendamentos

) {}
