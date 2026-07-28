package com.prontudigital.backend.prontuario.servicos;

import com.prontudigital.backend.prontuario.dto.AnamneseRequestDTO;
import com.prontudigital.backend.prontuario.dto.AnamneseResponseDTO;

import java.util.UUID;

public interface AnamneseService {
    AnamneseResponseDTO buscarPorPaciente(UUID pacienteUuid);
    AnamneseResponseDTO registrar(UUID pacienteUuid, AnamneseRequestDTO request);
    AnamneseResponseDTO atualizar(UUID pacienteUuid, AnamneseRequestDTO request);
}
