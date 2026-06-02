package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvolucaoClinicaRepository extends JpaRepository<EvolucaoClinica, Long> {

    Optional<EvolucaoClinica> findByAgendamento(Agendamento agendamento);
}
