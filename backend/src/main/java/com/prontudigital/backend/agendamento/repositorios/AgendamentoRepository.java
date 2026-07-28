package com.prontudigital.backend.agendamento.repositorios;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    boolean existsByProfissionalUuidAndPacienteUuid(UUID profissionalUuid, UUID pacienteUuid);

    boolean existsByProcedimentoId(Long procedimentoId);

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

    @Query("SELECT a FROM Agendamento a WHERE a.status IN :statuses AND a.inicioEm BETWEEN :inicio AND :fim")
    List<Agendamento> findByStatusInAndInicioEmBetween(
            @Param("statuses") List<StatusAgendamento> statuses,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query(value = """
            SELECT a.pacienteUuid AS pacienteUuid, MAX(a.inicioEm) AS ultimoAgendamento
            FROM Agendamento a
            WHERE a.inicioEm BETWEEN :inicio AND :fim
            AND (:status IS NULL OR a.status = :status)
            AND (:busca IS NULL OR a.pacienteUuid IN (
                SELECT u.uuid FROM Usuario u WHERE LOWER(u.nomeCompleto) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
            ))
            GROUP BY a.pacienteUuid
            ORDER BY MAX(a.inicioEm) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a.pacienteUuid)
            FROM Agendamento a
            WHERE a.inicioEm BETWEEN :inicio AND :fim
            AND (:status IS NULL OR a.status = :status)
            AND (:busca IS NULL OR a.pacienteUuid IN (
                SELECT u.uuid FROM Usuario u WHERE LOWER(u.nomeCompleto) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
            ))
            """)
    Page<PacienteAgendamentoResumo> buscarPacientesComAgendamentos(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusAgendamento status,
            @Param("busca") String busca,
            Pageable pageable);

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.pacienteUuid = :pacienteUuid
            AND a.inicioEm BETWEEN :inicio AND :fim
            AND (:status IS NULL OR a.status = :status)
            ORDER BY a.inicioEm DESC
            """)
    List<Agendamento> findByPacienteEPeriodo(
            @Param("pacienteUuid") UUID pacienteUuid,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusAgendamento status);

    /**
     * RF19/RF20 — atendimentos do periodo, com filtros opcionais.
     * Serve tanto a listagem do relatorio quanto a apuracao de ocupacao.
     */
    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.inicioEm BETWEEN :inicio AND :fim
            AND (:profissionalUuid IS NULL OR a.profissionalUuid = :profissionalUuid)
            AND (:status IS NULL OR a.status = :status)
            AND (:tipo IS NULL OR a.tipo = :tipo)
            ORDER BY a.inicioEm
            """)
    List<Agendamento> buscarParaRelatorio(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("profissionalUuid") UUID profissionalUuid,
            @Param("status") StatusAgendamento status,
            @Param("tipo") TipoAgendamento tipo);
}