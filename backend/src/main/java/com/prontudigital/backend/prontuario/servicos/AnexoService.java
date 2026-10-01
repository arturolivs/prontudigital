package com.prontudigital.backend.prontuario.servicos;

import com.prontudigital.backend.prontuario.dto.AnexoDownloadDTO;
import com.prontudigital.backend.prontuario.dto.AnexoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AnexoService {
    List<AnexoResponseDTO> listarPorPaciente(UUID pacienteUuid);
    AnexoResponseDTO enviar(UUID pacienteUuid, MultipartFile arquivo, UUID agendamentoUuid);
    AnexoDownloadDTO baixar(UUID pacienteUuid, UUID anexoUuid);
    void excluir(UUID pacienteUuid, UUID anexoUuid);
}
