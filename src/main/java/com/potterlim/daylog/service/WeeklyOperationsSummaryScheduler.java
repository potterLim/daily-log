package com.potterlim.daylog.service;

import java.time.LocalDate;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
    prefix = "day-log.operations.weekly-summary",
    name = "enabled",
    havingValue = "true"
)
public class WeeklyOperationsSummaryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyOperationsSummaryScheduler.class);

    private final WeeklyOperationsSummaryService mWeeklyOperationsSummaryService;
    private final IAlertNotificationService mAlertNotificationService;

    public WeeklyOperationsSummaryScheduler(
        WeeklyOperationsSummaryService weeklyOperationsSummaryService,
        IAlertNotificationService alertNotificationService
    ) {
        mWeeklyOperationsSummaryService = weeklyOperationsSummaryService;
        mAlertNotificationService = alertNotificationService;
    }

    @Scheduled(
        cron = "${day-log.operations.weekly-summary.cron:0 0 9 * * MON}",
        zone = "${day-log.operations.weekly-summary.zone:Asia/Seoul}"
    )
    @Transactional(readOnly = true)
    public void logPreviousWeekSummary() {
        LocalDate previousWeekStartDate = WeeklyOperationsSummaryService.resolvePreviousWeekStartDate(LocalDate.now());
        LocalDate previousWeekEndDate = previousWeekStartDate.plusDays(6L);

        try {
            WeeklyOperationsSummary weeklyOperationsSummary =
                mWeeklyOperationsSummaryService.buildWeeklySummary(previousWeekStartDate, previousWeekEndDate);
            LOGGER.info(
                "WEEKLY_OPERATIONS_SUMMARY weekStart={} weekEnd={} totalRegisteredUsers={} newlyRegisteredUsers={} "
                    + "weeklyActiveUsers={} weeklyWritingDays={} weeklyMorningLogs={} weeklyEveningLogs={} "
                    + "averageWritingDaysPerActiveUser={} averageLogCompletionsPerActiveUser={} goalCompletionRatePercent={}",
                weeklyOperationsSummary.getWeekStartDate(),
                weeklyOperationsSummary.getWeekEndDate(),
                weeklyOperationsSummary.getTotalRegisteredUsers(),
                weeklyOperationsSummary.getNewlyRegisteredUsers(),
                weeklyOperationsSummary.getWeeklyActiveUsers(),
                weeklyOperationsSummary.getWeeklyWritingDays(),
                weeklyOperationsSummary.getWeeklyMorningLogs(),
                weeklyOperationsSummary.getWeeklyEveningLogs(),
                formatMetric(weeklyOperationsSummary.getAverageWritingDaysPerActiveUser()),
                formatMetric(weeklyOperationsSummary.getAverageLogCompletionsPerActiveUser()),
                formatMetric(weeklyOperationsSummary.getGoalCompletionRatePercent())
            );
        } catch (RuntimeException runtimeException) {
            LOGGER.error(
                "Weekly operations summary generation failed. weekStart={}, weekEnd={}",
                previousWeekStartDate,
                previousWeekEndDate,
                runtimeException
            );
            mAlertNotificationService.sendOperationalAlert(
                "weekly-operations-summary-failed",
                "weekStart=%s, weekEnd=%s".formatted(previousWeekStartDate, previousWeekEndDate)
            );
        }
    }

    private static String formatMetric(double metricValue) {
        return String.format(Locale.US, "%.2f", metricValue);
    }
}
