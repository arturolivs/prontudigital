package com.prontudigital.backend.agendamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Requisicao de reagendamento")
public record ReagendarRequestDTO(

        @NotNull
        @Schema(description = "Novo inicio", example = "2026-05-20T14:00:00")
        LocalDateTime novoInicioEm,

        @NotNull
        @Schema(description = "Novo fim", example = "2026-05-20T15:00:00")
        LocalDateTime novoFimEm,

        @Schema(description = "Motivo do reagendamento")
        String motivo

) {}