package com.prontudigital.backend.prontuario.servicos;

import com.prontudigital.backend.prontuario.dto.PrescricaoRequestDTO;
import com.prontudigital.backend.prontuario.dto.PrescricaoResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PrescricaoService {
    List<PrescricaoResponseDTO> listarPorPaciente(UUID pacienteUuid);
    PrescricaoResponseDTO registrar(UUID pacienteUuid, PrescricaoRequestDTO request);
    PrescricaoResponseDTO atualizar(UUID pacienteUuid, UUID prescricaoUuid, PrescricaoRequestDTO request);
    void excluir(UUID pacienteUuid, UUID prescricaoUuid);
}
