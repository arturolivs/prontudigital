package com.prontudigital.backend.prontuario.dto;

import com.prontudigital.backend.prontuario.enums.TipoPrescricao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Prescricao de medicamento ou cuidado de enfermagem (RF16)")
public record PrescricaoResponseDTO(

        UUID uuid,
        UUID pacienteUuid,
        String pacienteNome,
        UUID agendamentoUuid,
        TipoPrescricao tipo,
        String descricao,
        String posologia,
        String frequencia,
        String duracao,
        String orientacoes,
        UUID registradoPor,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm

) {}
