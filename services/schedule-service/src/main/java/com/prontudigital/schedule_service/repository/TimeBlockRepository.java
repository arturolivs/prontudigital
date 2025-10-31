package com.prontudigital.schedule_service.repository;

import com.prontudigital.schedule_service.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {

    @Query("SELECT tb FROM TimeBlock tb WHERE " +
            "tb.professionalUuid = :professionalUuid AND " +
            "((tb.startDateTime < :endDateTime AND tb.endDateTime > :startDateTime))")
    List<TimeBlock> findConflictingTimeBlocks(
            @Param("professionalUuid") UUID professionalUuid,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);
}