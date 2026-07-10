package com.prontudigital.backend.prontuario.repositorios;

import com.prontudigital.backend.prontuario.entidades.Anexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    List<Anexo> findByPacienteUuidOrderByCriadoEmDesc(UUID pacienteUuid);

    Optional<Anexo> findByUuidAndPacienteUuid(UUID uuid, UUID pacienteUuid);
}
