package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;

import java.time.LocalDate;
import java.util.List;

public interface AgendamentoService {
    AgendamentoResponseDTO agendar(AgendamentoRequestDTO request);
    void cancelar(Long agendamentoId);
    void concluir(Long agendamentoId);
    List<AgendamentoViewDTO> visualizarAgenda(LocalDate data, String tipoVisualizacao);
    List<AgendamentoViewDTO> getTratamentosPorAvaliacao(Long avaliacaoId);
}
