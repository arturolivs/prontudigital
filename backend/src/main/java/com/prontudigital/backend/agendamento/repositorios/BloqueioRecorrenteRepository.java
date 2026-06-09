package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.BloqueioRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BloqueioRecorrenteRepository extends JpaRepository<BloqueioRecorrente, Long> {

    List<BloqueioRecorrente> findByProfissionalUuidAndAtivoTrue(UUID profissionalUuid);
}
