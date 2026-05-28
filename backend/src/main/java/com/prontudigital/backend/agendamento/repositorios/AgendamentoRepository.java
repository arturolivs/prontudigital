package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByIdAndPacienteUuid(Long id, UUID pacienteUuid);

    List<Agendamento> findByProfissionalUuidAndInicioEmBetween(
            UUID profissionalUuid, LocalDateTime inicio, LocalDateTime fim);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Agendamento a WHERE " +
            "a.profissionalUuid = :profissionalUuid AND " +
            "a.status <> 'CANCELADO' AND " +
            "(a.inicioEm < :fim AND a.fimEm > :inicio)")
    List<Agendamento> findConflitosParaProfissionalComLock(
            @Param("profissionalUuid") UUID profissionalUuid,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Agendamento a WHERE " +
            "a.pacienteUuid = :pacienteUuid AND " +
            "a.status <> 'CANCELADO' AND " +
            "(a.inicioEm < :fim AND a.fimEm > :inicio)")
    List<Agendamento> findConflitosParaPacienteComLock(
            @Param("pacienteUuid") UUID pacienteUuid,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT a FROM Agendamento a WHERE " +
            "a.profissionalUuid = :profissionalUuid AND " +
            "a.status NOT IN ('CANCELADO', 'REALIZADO', 'NAO_COMPARECEU') AND " +
            "a.inicioEm < :fim AND a.fimEm > :inicio")
    List<Agendamento> findOcupadosPorProfissional(
            @Param("profissionalUuid") UUID profissionalUuid,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    List<Agendamento> findByAvaliacaoId(Long avaliacaoId);

    List<Agendamento> findByPacienteUuidOrderByInicioEmDesc(UUID pacienteUuid);
}