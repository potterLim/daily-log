package com.potterlim.daymark.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.potterlim.daymark.entity.DaymarkEntry;
import com.potterlim.daymark.entity.EUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IDaymarkEntryRepository extends JpaRepository<DaymarkEntry, Long> {

    @Query("""
        select daymarkEntry
        from DaymarkEntry daymarkEntry
        where daymarkEntry.mUserAccount.mId = :userAccountId
        and daymarkEntry.mEntryDate = :entryDate
        """)
    Optional<DaymarkEntry> findByUserAccountIdAndLogDate(
        @Param("userAccountId") Long userAccountId,
        @Param("entryDate") LocalDate entryDate
    );

    @Query("""
        select daymarkEntry
        from DaymarkEntry daymarkEntry
        where daymarkEntry.mUserAccount.mId = :userAccountId
        and daymarkEntry.mEntryDate between :startDate and :endDate
        order by daymarkEntry.mEntryDate asc
        """)
    List<DaymarkEntry> findWeekEntries(
        @Param("userAccountId") Long userAccountId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        select daymarkEntry
        from DaymarkEntry daymarkEntry
        where daymarkEntry.mUserAccount.mId = :userAccountId
        and daymarkEntry.mEntryDate between :startDate and :endDate
        order by daymarkEntry.mEntryDate asc
        """)
    List<DaymarkEntry> findEntriesByUserAccountIdWithinDateRange(
        @Param("userAccountId") Long userAccountId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        select daymarkEntry
        from DaymarkEntry daymarkEntry
        join fetch daymarkEntry.mUserAccount
        where daymarkEntry.mEntryDate between :startDate and :endDate
            and daymarkEntry.mUserAccount.mUserRole <> :excludedUserRole
        order by daymarkEntry.mEntryDate asc
        """)
    List<DaymarkEntry> findEntriesWithinDateRangeExcludingUserRole(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedUserRole") EUserRole excludedUserRole
    );
}
