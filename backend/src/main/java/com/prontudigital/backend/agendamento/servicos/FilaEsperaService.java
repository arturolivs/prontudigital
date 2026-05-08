package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.FilaEsperaDTO;
import com.prontudigital.backend.agendamento.dto.FilaEsperaRequestDTO;
import java.util.List;
import java.util.UUID;

public interface FilaEsperaService {
    FilaEsperaDTO entrar(FilaEsperaRequestDTO request);
    void sair(Long id);
    void marcarComoNotificado(Long id);
    List<FilaEsperaDTO> listarFilaDoProfissional(UUID profissionalUuid);
}