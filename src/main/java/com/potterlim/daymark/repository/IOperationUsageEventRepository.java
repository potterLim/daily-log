package com.potterlim.daymark.repository;

import java.time.LocalDateTime;
import java.util.List;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.OperationUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IOperationUsageEventRepository extends JpaRepository<OperationUsageEvent, Long> {

    @Query("""
        select count(operationUsageEvent)
        from OperationUsageEvent operationUsageEvent
        where operationUsageEvent.mEventType = :eventType
            and operationUsageEvent.mOccurredAt >= :startDateTime
            and operationUsageEvent.mOccurredAt < :endExclusiveDateTime
        """)
    long countByEventTypeWithin(
        @Param("eventType") EOperationEventType eventType,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime
    );

    @Query("""
        select distinct operationUsageEvent.mUserAccountId
        from OperationUsageEvent operationUsageEvent
        where operationUsageEvent.mOccurredAt >= :startDateTime
            and operationUsageEvent.mOccurredAt < :endExclusiveDateTime
            and operationUsageEvent.mUserAccountId is not null
        """)
    List<Long> findDistinctUserAccountIdsWithin(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime
    );
}
