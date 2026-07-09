package com.prontudigital.backend.prontuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Anamnese do paciente (RF13)")
public record AnamneseResponseDTO(

        UUID uuid,
        UUID pacienteUuid,
        String pacienteNome,
        String queixaPrincipal,
        String historicoDoencaAtual,
        String historicoMedicoPregresso,
        String alergias,
        String medicamentosEmUso,
        String historicoFamiliar,
        String habitos,
        String observacoes,
        UUID registradoPor,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm

) {}
