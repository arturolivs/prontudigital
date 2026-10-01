package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoCurativo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvolucaoCurativoRepository extends JpaRepository<EvolucaoCurativo, Long> {

    Optional<EvolucaoCurativo> findByAgendamento(Agendamento agendamento);
}
