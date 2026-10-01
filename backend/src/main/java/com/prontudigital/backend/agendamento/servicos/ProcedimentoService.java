package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.ProcedimentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.ProcedimentoResponseDTO;

import java.util.List;

public interface ProcedimentoService {

    List<ProcedimentoResponseDTO> listar(boolean incluirInativos);

    ProcedimentoResponseDTO buscarPorId(Long id);

    ProcedimentoResponseDTO criar(ProcedimentoRequestDTO request);

    ProcedimentoResponseDTO atualizar(Long id, ProcedimentoRequestDTO request);

    void excluir(Long id);
}
