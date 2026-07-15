package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoEnfermagemRequestDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoTratamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.PacienteAgendamentosDTO;
import com.prontudigital.backend.agendamento.dto.ReagendarRequestDTO;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AgendamentoService {
    AgendamentoDetalhadoDTO buscarPorId(Long id);
    AgendamentoResponseDTO agendar(AgendamentoRequestDTO request);
    AgendamentoResponseDTO reagendar(Long agendamentoId, ReagendarRequestDTO request);
    void confirmar(Long agendamentoId);
    void cancelar(Long agendamentoId);
    void concluir(Long agendamentoId);
    AgendamentoDetalhadoDTO registrarEvolucao(Long id, EvolucaoTratamentoRequestDTO request);
    AgendamentoDetalhadoDTO registrarEvolucaoEnfermagem(Long id, EvolucaoEnfermagemRequestDTO request);
    List<AgendamentoViewDTO> visualizarAgenda(LocalDate data,
                                              TipoVisualizacaoAgenda tipo,
                                              UUID profissionalUuidOpcional);
    List<AgendamentoViewDTO> getTratamentosPorAvaliacao(Long avaliacaoId);
    List<AgendamentoViewDTO> obterMeusAgendamentos();
    Page<PacienteAgendamentosDTO> listarPacientesComAgendamentos(String busca, StatusAgendamento status, Pageable pageable);
}
