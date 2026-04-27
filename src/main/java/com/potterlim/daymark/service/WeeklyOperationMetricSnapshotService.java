package com.potterlim.daymark.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.potterlim.daymark.entity.WeeklyOperationMetricSnapshot;
import com.potterlim.daymark.repository.IWeeklyOperationMetricSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyOperationMetricSnapshotService {

    private final IWeeklyOperationMetricSnapshotRepository mWeeklyOperationMetricSnapshotRepository;
    private final Clock mClock;

    public WeeklyOperationMetricSnapshotService(
        IWeeklyOperationMetricSnapshotRepository weeklyOperationMetricSnapshotRepository,
        Clock clock
    ) {
        mWeeklyOperationMetricSnapshotRepository = weeklyOperationMetricSnapshotRepository;
        mClock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WeeklyOperationMetricSnapshot saveWeeklySnapshot(WeeklyOperationsSummary weeklyOperationsSummary) {
        if (weeklyOperationsSummary == null) {
            throw new IllegalArgumentException("weeklyOperationsSummary must not be null.");
        }

        WeeklyOperationMetricSnapshot weeklyOperationMetricSnapshot = mWeeklyOperationMetricSnapshotRepository
            .findByWeekStartDateAndWeekEndDate(
                weeklyOperationsSummary.getWeekStartDate(),
                weeklyOperationsSummary.getWeekEndDate()
            )
            .orElseGet(() -> WeeklyOperationMetricSnapshot.create(
                weeklyOperationsSummary.getWeekStartDate(),
                weeklyOperationsSummary.getWeekEndDate()
            ));

        weeklyOperationMetricSnapshot.updateFrom(weeklyOperationsSummary, LocalDateTime.now(mClock));
        return mWeeklyOperationMetricSnapshotRepository.save(weeklyOperationMetricSnapshot);
    }

    @Transactional(readOnly = true)
    public List<WeeklyOperationMetricSnapshot> listWeeklySnapshotsWithinDateRange(
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate must not be null.");
        }

        if (endDate == null) {
            throw new IllegalArgumentException("endDate must not be null.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate.");
        }

        return mWeeklyOperationMetricSnapshotRepository.findWeeklySnapshotsWithinDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<WeeklyOperationMetricSnapshot> listRecentWeeklySnapshots() {
        return mWeeklyOperationMetricSnapshotRepository.findRecentWeeklySnapshots(PageRequest.of(0, 12));
    }
}
