package com.prontudigital.backend.prontuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Metadados de um anexo (exame/documento) do prontuario (RF15)")
public record AnexoResponseDTO(

        UUID uuid,
        UUID pacienteUuid,
        String pacienteNome,
        UUID agendamentoUuid,
        String nomeOriginal,
        String tipoConteudo,
        long tamanhoBytes,
        UUID registradoPor,
        LocalDateTime criadoEm

) {}
