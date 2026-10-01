package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.HistoricoAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoricoAgendamentoRepository
        extends JpaRepository<HistoricoAgendamento, Long> {
    List<HistoricoAgendamento> findByAgendamentoIdOrderByAlteradoEmDesc(Long agendamentoId);
}
