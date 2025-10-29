package com.prontudigital.schedule_service.repository;

import com.prontudigital.schedule_service.model.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {

    @Query("SELECT tb FROM TimeBlock tb WHERE " +
            "tb.professionalId = :professionalId AND " +
            "((tb.startDateTime < :endDateTime AND tb.endDateTime > :startDateTime))")
    List<TimeBlock> findConflictingTimeBlocks(
            @Param("professionalId") Long professionalId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);
}