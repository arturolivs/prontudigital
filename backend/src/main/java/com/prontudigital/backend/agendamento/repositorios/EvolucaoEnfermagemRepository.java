package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoEnfermagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvolucaoEnfermagemRepository extends JpaRepository<EvolucaoEnfermagem, Long> {

    Optional<EvolucaoEnfermagem> findByAgendamento(Agendamento agendamento);
}
