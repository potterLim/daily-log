package com.potterlim.daymark.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.EUserRole;
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
            and (
                operationUsageEvent.mUserAccountId is null
                or operationUsageEvent.mUserAccountId not in (
                    select userAccount.mId
                    from UserAccount userAccount
                    where userAccount.mUserRole = :excludedUserRole
                )
            )
        """)
    long countByEventTypeWithinExcludingUserRole(
        @Param("eventType") EOperationEventType eventType,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime,
        @Param("excludedUserRole") EUserRole excludedUserRole
    );

    @Query("""
        select distinct operationUsageEvent.mUserAccountId
        from OperationUsageEvent operationUsageEvent
        where operationUsageEvent.mOccurredAt >= :startDateTime
            and operationUsageEvent.mOccurredAt < :endExclusiveDateTime
            and operationUsageEvent.mUserAccountId is not null
            and operationUsageEvent.mUserAccountId in (
                select userAccount.mId
                from UserAccount userAccount
                where userAccount.mUserRole <> :excludedUserRole
            )
        """)
    List<Long> findDistinctUserAccountIdsWithinExcludingUserRole(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime,
        @Param("excludedUserRole") EUserRole excludedUserRole
    );

    @Query("""
        select count(distinct operationUsageEvent.mUserAccountId)
        from OperationUsageEvent operationUsageEvent
        where operationUsageEvent.mEventType in :eventTypes
            and operationUsageEvent.mOccurredAt >= :startDateTime
            and operationUsageEvent.mOccurredAt < :endExclusiveDateTime
            and operationUsageEvent.mUserAccountId is not null
            and operationUsageEvent.mUserAccountId in (
                select userAccount.mId
                from UserAccount userAccount
                where userAccount.mUserRole <> :excludedUserRole
            )
        """)
    long countDistinctUserAccountIdsByEventTypesWithinExcludingUserRole(
        @Param("eventTypes") Collection<EOperationEventType> eventTypes,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime,
        @Param("excludedUserRole") EUserRole excludedUserRole
    );
}
