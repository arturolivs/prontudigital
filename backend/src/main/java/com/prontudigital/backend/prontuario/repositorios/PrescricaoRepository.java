package com.prontudigital.backend.prontuario.repositorios;

import com.prontudigital.backend.prontuario.entidades.Prescricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescricaoRepository extends JpaRepository<Prescricao, Long> {

    List<Prescricao> findByPacienteUuidOrderByCriadoEmDesc(UUID pacienteUuid);

    Optional<Prescricao> findByUuidAndPacienteUuid(UUID uuid, UUID pacienteUuid);
}
