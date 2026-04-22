package com.potterlim.daylog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.potterlim.daylog.entity.DailyLogEntry;
import com.potterlim.daylog.repository.IDailyLogEntryRepository;
import com.potterlim.daylog.repository.IUserAccountRepository;
import com.potterlim.daylog.support.EDailyLogSectionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyOperationsSummaryService {

    private final IUserAccountRepository mUserAccountRepository;
    private final IDailyLogEntryRepository mDailyLogEntryRepository;

    public WeeklyOperationsSummaryService(
        IUserAccountRepository userAccountRepository,
        IDailyLogEntryRepository dailyLogEntryRepository
    ) {
        mUserAccountRepository = userAccountRepository;
        mDailyLogEntryRepository = dailyLogEntryRepository;
    }

    @Transactional(readOnly = true)
    public WeeklyOperationsSummary buildWeeklySummary(LocalDate weekStartDate, LocalDate weekEndDate) {
        validateWeekRange(weekStartDate, weekEndDate);

        List<DailyLogEntry> weeklyEntries = mDailyLogEntryRepository.findEntriesWithinDateRange(weekStartDate, weekEndDate);
        Set<Long> weeklyActiveUserIds = new HashSet<>();
        long weeklyMorningLogs = 0L;
        long weeklyEveningLogs = 0L;
        int totalTrackedGoals = 0;
        int completedTrackedGoals = 0;

        for (DailyLogEntry dailyLogEntry : weeklyEntries) {
            weeklyActiveUserIds.add(dailyLogEntry.getUserAccountId().getValue());

            if (dailyLogEntry.hasMorningLog()) {
                weeklyMorningLogs += 1L;
            }

            if (dailyLogEntry.hasEveningLog()) {
                weeklyEveningLogs += 1L;
            }

            GoalCompletionAccumulator goalCompletionAccumulator =
                analyzeGoalCompletion(dailyLogEntry.readSection(EDailyLogSectionType.EVENING_GOALS));
            totalTrackedGoals += goalCompletionAccumulator.getTotalGoals();
            completedTrackedGoals += goalCompletionAccumulator.getCompletedGoals();
        }

        long weeklyActiveUsers = weeklyActiveUserIds.size();
        long weeklyWritingDays = weeklyEntries.size();
        long totalRegisteredUsers = mUserAccountRepository.count();
        long newlyRegisteredUsers = mUserAccountRepository.countCreatedWithin(
            weekStartDate.atStartOfDay(),
            weekEndDate.plusDays(1L).atStartOfDay()
        );

        double averageWritingDaysPerActiveUser = weeklyActiveUsers == 0
            ? 0.0
            : (double) weeklyWritingDays / weeklyActiveUsers;
        double averageLogCompletionsPerActiveUser = weeklyActiveUsers == 0
            ? 0.0
            : (double) (weeklyMorningLogs + weeklyEveningLogs) / weeklyActiveUsers;
        double goalCompletionRatePercent = totalTrackedGoals == 0
            ? 0.0
            : (double) completedTrackedGoals * 100.0 / totalTrackedGoals;

        return new WeeklyOperationsSummary(
            weekStartDate,
            weekEndDate,
            totalRegisteredUsers,
            newlyRegisteredUsers,
            weeklyActiveUsers,
            weeklyWritingDays,
            weeklyMorningLogs,
            weeklyEveningLogs,
            averageWritingDaysPerActiveUser,
            averageLogCompletionsPerActiveUser,
            goalCompletionRatePercent
        );
    }

    static LocalDate resolvePreviousWeekStartDate(LocalDate currentDate) {
        return currentDate.minusWeeks(1L).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static void validateWeekRange(LocalDate weekStartDate, LocalDate weekEndDate) {
        if (weekStartDate == null) {
            throw new IllegalArgumentException("weekStartDate must not be null.");
        }

        if (weekEndDate == null) {
            throw new IllegalArgumentException("weekEndDate must not be null.");
        }

        if (weekEndDate.isBefore(weekStartDate)) {
            throw new IllegalArgumentException("weekEndDate must not be before weekStartDate.");
        }
    }

    private static GoalCompletionAccumulator analyzeGoalCompletion(String eveningGoalsText) {
        GoalCompletionAccumulator goalCompletionAccumulator = new GoalCompletionAccumulator();

        if (eveningGoalsText == null || eveningGoalsText.isBlank()) {
            return goalCompletionAccumulator;
        }

        for (String line : splitLines(eveningGoalsText)) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("- [x]") || trimmedLine.startsWith("- [X]")) {
                goalCompletionAccumulator.incrementCompletedGoals();
                continue;
            }

            if (trimmedLine.startsWith("- [ ]")) {
                goalCompletionAccumulator.incrementPendingGoals();
            }
        }

        return goalCompletionAccumulator;
    }

    private static String[] splitLines(String textOrNull) {
        if (textOrNull == null || textOrNull.isEmpty()) {
            return new String[0];
        }

        return textOrNull.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private static final class GoalCompletionAccumulator {

        private int mTotalGoals;
        private int mCompletedGoals;

        public int getTotalGoals() {
            return mTotalGoals;
        }

        public int getCompletedGoals() {
            return mCompletedGoals;
        }

        public void incrementCompletedGoals() {
            mTotalGoals += 1;
            mCompletedGoals += 1;
        }

        public void incrementPendingGoals() {
            mTotalGoals += 1;
        }
    }
}
