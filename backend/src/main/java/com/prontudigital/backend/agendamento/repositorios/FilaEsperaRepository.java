package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.FilaEspera;
import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FilaEsperaRepository extends JpaRepository<FilaEspera, Long> {

    List<FilaEspera> findByProfissionalUuidAndStatusOrderByPrioridadeAscCreatedAtAsc(
            UUID profissionalUuid, StatusFilaEspera status);

    List<FilaEspera> findByPacienteUuid(UUID pacienteUuid);
}