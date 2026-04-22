package com.potterlim.daylog.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.potterlim.daylog.entity.DailyLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IDailyLogEntryRepository extends JpaRepository<DailyLogEntry, Long> {

    @Query("""
        select dailyLogEntry
        from DailyLogEntry dailyLogEntry
        where dailyLogEntry.mUserAccount.mId = :userAccountId
        and dailyLogEntry.mLogDate = :logDate
        """)
    Optional<DailyLogEntry> findByUserAccountIdAndLogDate(
        @Param("userAccountId") Long userAccountId,
        @Param("logDate") LocalDate logDate
    );

    @Query("""
        select dailyLogEntry
        from DailyLogEntry dailyLogEntry
        where dailyLogEntry.mUserAccount.mId = :userAccountId
        and dailyLogEntry.mLogDate between :startDate and :endDate
        order by dailyLogEntry.mLogDate asc
        """)
    List<DailyLogEntry> findWeekEntries(
        @Param("userAccountId") Long userAccountId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        select dailyLogEntry
        from DailyLogEntry dailyLogEntry
        join fetch dailyLogEntry.mUserAccount
        where dailyLogEntry.mLogDate between :startDate and :endDate
        order by dailyLogEntry.mLogDate asc
        """)
    List<DailyLogEntry> findEntriesWithinDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
