package com.prontudigital.backend.agendamento.servicos.impl.fixtures;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.enums.TipoBloqueio;

import java.time.LocalDateTime;
import java.util.UUID;

public final class BloqueioHorarioTestFixtures {

    public static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID OUTRO_PROFISSIONAL_UUID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 6, 1, 14, 0);
    public static final LocalDateTime FIM = LocalDateTime.of(2026, 6, 1, 16, 0);

    private BloqueioHorarioTestFixtures() {}

    public static BloqueioHorarioDTO requestValido() {
        return new BloqueioHorarioDTO(
                null, null, PROFISSIONAL_UUID,
                INICIO, FIM, "Ferias", TipoBloqueio.INDISPONIVEL);
    }

    public static BloqueioHorarioDTO requestComProfissional(UUID profissionalUuid) {
        return new BloqueioHorarioDTO(
                null, null, profissionalUuid,
                INICIO, FIM, "Ferias", TipoBloqueio.INDISPONIVEL);
    }

    public static BloqueioHorario bloqueioSalvo() {
        return BloqueioHorario.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .profissionalUuid(PROFISSIONAL_UUID)
                .inicioEm(INICIO)
                .fimEm(FIM)
                .motivo("Ferias")
                .tipo(TipoBloqueio.INDISPONIVEL)
                .build();
    }
}