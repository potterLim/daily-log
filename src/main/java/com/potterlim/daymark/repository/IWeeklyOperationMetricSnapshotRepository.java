package com.potterlim.daymark.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.potterlim.daymark.entity.WeeklyOperationMetricSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IWeeklyOperationMetricSnapshotRepository extends JpaRepository<WeeklyOperationMetricSnapshot, Long> {

    @Query("""
        select weeklyOperationMetricSnapshot
        from WeeklyOperationMetricSnapshot weeklyOperationMetricSnapshot
        where weeklyOperationMetricSnapshot.mWeekStartDate = :weekStartDate
            and weeklyOperationMetricSnapshot.mWeekEndDate = :weekEndDate
        """)
    Optional<WeeklyOperationMetricSnapshot> findByWeekStartDateAndWeekEndDate(
        @Param("weekStartDate") LocalDate weekStartDate,
        @Param("weekEndDate") LocalDate weekEndDate
    );

    @Query("""
        select weeklyOperationMetricSnapshot
        from WeeklyOperationMetricSnapshot weeklyOperationMetricSnapshot
        where weeklyOperationMetricSnapshot.mWeekStartDate >= :startDate
            and weeklyOperationMetricSnapshot.mWeekStartDate <= :endDate
        order by weeklyOperationMetricSnapshot.mWeekStartDate desc
        """)
    List<WeeklyOperationMetricSnapshot> findWeeklySnapshotsWithinDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        select weeklyOperationMetricSnapshot
        from WeeklyOperationMetricSnapshot weeklyOperationMetricSnapshot
        order by weeklyOperationMetricSnapshot.mWeekStartDate desc
        """)
    List<WeeklyOperationMetricSnapshot> findRecentWeeklySnapshots(Pageable pageable);
}
