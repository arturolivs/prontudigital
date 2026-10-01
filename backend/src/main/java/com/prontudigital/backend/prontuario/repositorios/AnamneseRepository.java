package com.prontudigital.backend.prontuario.repositorios;

import com.prontudigital.backend.prontuario.entidades.Anamnese;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnamneseRepository extends JpaRepository<Anamnese, Long> {

    Optional<Anamnese> findByPacienteUuid(UUID pacienteUuid);

    boolean existsByPacienteUuid(UUID pacienteUuid);
}
