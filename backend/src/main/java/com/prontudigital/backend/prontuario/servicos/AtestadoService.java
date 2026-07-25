package com.prontudigital.backend.prontuario.servicos;

import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.prontuario.dto.AtestadoRequestDTO;
import com.prontudigital.backend.prontuario.dto.AtestadoResponseDTO;

import java.util.List;
import java.util.UUID;

/** Emissao de atestados ao paciente (RF17). */
public interface AtestadoService {

    List<AtestadoResponseDTO> listarPorPaciente(UUID pacienteUuid);

    AtestadoResponseDTO emitir(UUID pacienteUuid, AtestadoRequestDTO request);

    /** Regera o PDF a partir do registro — nada de binario persistido. */
    ArquivoGerado gerarPdf(UUID pacienteUuid, UUID atestadoUuid);

    void remover(UUID pacienteUuid, UUID atestadoUuid);
}
