package com.prontudigital.backend.prontuario.dto;

import com.prontudigital.backend.prontuario.enums.TipoAtestado;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Atestado emitido (RF17)")
@Builder
public record AtestadoResponseDTO(

        UUID uuid,

        UUID pacienteUuid,

        String nomePaciente,

        UUID agendamentoUuid,

        TipoAtestado tipo,

        Integer diasAfastamento,

        String cid,

        String observacoes,

        UUID emitidoPor,

        String nomeProfissional,

        LocalDateTime criadoEm

) {}
