package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BloqueioHorarioRepository extends JpaRepository<BloqueioHorario, Long> {

    @Query("SELECT b FROM BloqueioHorario b WHERE " +
            "b.profissionalUuid = :profissionalUuid AND " +
            "(b.inicioEm < :fimEm AND b.fimEm > :inicioEm)")
    List<BloqueioHorario> findConflitos(
            @Param("profissionalUuid") UUID profissionalUuid,
            @Param("inicioEm") LocalDateTime inicioEm,
            @Param("fimEm") LocalDateTime fimEm);
}