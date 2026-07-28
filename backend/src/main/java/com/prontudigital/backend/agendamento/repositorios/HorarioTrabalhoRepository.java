package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.HorarioTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    List<HorarioTrabalho> findByProfissionalUuidAndAtivoTrue(UUID profissionalUuid);

    List<HorarioTrabalho> findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
            UUID profissionalUuid, Integer diaSemana);

    boolean existsByProfissionalUuidAndAtivoTrue(UUID profissionalUuid);
}
