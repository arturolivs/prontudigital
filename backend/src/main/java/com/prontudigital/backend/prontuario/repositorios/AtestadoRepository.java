package com.prontudigital.backend.prontuario.repositorios;

import com.prontudigital.backend.prontuario.entidades.Atestado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AtestadoRepository extends JpaRepository<Atestado, Long> {

    List<Atestado> findByPacienteUuidOrderByCriadoEmDesc(UUID pacienteUuid);

    Optional<Atestado> findByUuidAndPacienteUuid(UUID uuid, UUID pacienteUuid);
}
