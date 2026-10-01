package com.prontudigital.backend.agendamento.servicos.impl.fixtures;

import com.prontudigital.backend.agendamento.dto.FilaEsperaRequestDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.FilaEspera;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.eventos.AgendamentoCanceladoEvento;

import java.time.LocalDateTime;
import java.util.UUID;

public final class FilaEsperaTestFixtures {

    public static final UUID PACIENTE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID OUTRO_UUID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private FilaEsperaTestFixtures() {}

    public static FilaEsperaRequestDTO requestValido() {
        return new FilaEsperaRequestDTO(
                PACIENTE_UUID, PROFISSIONAL_UUID,
                TipoAgendamento.AVALIACAO,
                LocalDateTime.of(2026, 6, 1, 14, 0));
    }

    public static FilaEsperaRequestDTO requestComPaciente(UUID pacienteUuid) {
        return new FilaEsperaRequestDTO(
                pacienteUuid, PROFISSIONAL_UUID,
                TipoAgendamento.AVALIACAO,
                LocalDateTime.of(2026, 6, 1, 14, 0));
    }

    public static FilaEspera filaAtiva() {
        return FilaEspera.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .pacienteUuid(PACIENTE_UUID)
                .profissionalUuid(PROFISSIONAL_UUID)
                .tipoPreferido(TipoAgendamento.AVALIACAO)
                .prioridade(0)
                .status(StatusFilaEspera.ATIVO)
                .build();
    }

    public static AgendamentoCanceladoEvento eventoCancelamento() {
        Agendamento agendamento = Agendamento.builder()
                .id(1L)
                .profissionalUuid(PROFISSIONAL_UUID)
                .pacienteUuid(OUTRO_UUID)
                .status(StatusAgendamento.CANCELADO)
                .build();
        return new AgendamentoCanceladoEvento(agendamento);
    }
}