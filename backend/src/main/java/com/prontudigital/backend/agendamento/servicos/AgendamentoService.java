package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.ReagendarRequestDTO;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AgendamentoService {
    AgendamentoResponseDTO agendar(AgendamentoRequestDTO request);
    AgendamentoResponseDTO reagendar(Long agendamentoId, ReagendarRequestDTO request);
    void cancelar(Long agendamentoId);
    void concluir(Long agendamentoId);
    List<AgendamentoViewDTO> visualizarAgenda(LocalDate data,
                                              TipoVisualizacaoAgenda tipo,
                                              UUID profissionalUuidOpcional);
    List<AgendamentoViewDTO> getTratamentosPorAvaliacao(Long avaliacaoId);

    List<AgendamentoViewDTO> obterMeusAgendamentos();
}
