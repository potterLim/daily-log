package com.potterlim.daymark.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.potterlim.daymark.entity.DaymarkEntry;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.EUserRole;
import com.potterlim.daymark.repository.IDaymarkEntryRepository;
import com.potterlim.daymark.repository.IOperationUsageEventRepository;
import com.potterlim.daymark.repository.IUserAccountRepository;
import com.potterlim.daymark.support.EDaymarkSectionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyOperationsSummaryService {

    private static final EUserRole EXCLUDED_OPERATION_USER_ROLE = EUserRole.ADMIN;

    private final IUserAccountRepository mUserAccountRepository;
    private final IDaymarkEntryRepository mDaymarkEntryRepository;
    private final IOperationUsageEventRepository mOperationUsageEventRepository;

    public WeeklyOperationsSummaryService(
        IUserAccountRepository userAccountRepository,
        IDaymarkEntryRepository daymarkEntryRepository,
        IOperationUsageEventRepository operationUsageEventRepository
    ) {
        mUserAccountRepository = userAccountRepository;
        mDaymarkEntryRepository = daymarkEntryRepository;
        mOperationUsageEventRepository = operationUsageEventRepository;
    }

    @Transactional(readOnly = true)
    public WeeklyOperationsSummary buildWeeklySummary(LocalDate weekStartDate, LocalDate weekEndDate) {
        validateWeekRange(weekStartDate, weekEndDate);

        LocalDateTime weekStartDateTime = weekStartDate.atStartOfDay();
        LocalDateTime weekEndExclusiveDateTime = weekEndDate.plusDays(1L).atStartOfDay();
        List<DaymarkEntry> weeklyEntries = mDaymarkEntryRepository.findEntriesWithinDateRangeExcludingUserRole(
            weekStartDate,
            weekEndDate,
            EXCLUDED_OPERATION_USER_ROLE
        );
        Set<Long> weeklyWritingUserIds = new HashSet<>();
        Set<Long> newWorkspaceActivatedUserIds = new HashSet<>();
        long weeklyWritingDays = 0L;
        long weeklyMorningEntries = 0L;
        long weeklyEveningEntries = 0L;
        long weeklyPlanReviewCompletedDays = 0L;
        int totalTrackedGoals = 0;
        int completedTrackedGoals = 0;

        for (DaymarkEntry daymarkEntry : weeklyEntries) {
            if (!daymarkEntry.hasAnyEntryContent()) {
                continue;
            }

            weeklyWritingUserIds.add(daymarkEntry.getUserAccountId().getValue());
            weeklyWritingDays += 1L;

            boolean hasMorningEntry = daymarkEntry.hasMorningEntry();
            boolean hasEveningEntry = daymarkEntry.hasEveningEntry();
            if (hasMorningEntry) {
                weeklyMorningEntries += 1L;
            }

            if (hasEveningEntry) {
                weeklyEveningEntries += 1L;
            }

            if (hasMorningEntry && hasEveningEntry) {
                weeklyPlanReviewCompletedDays += 1L;
            }

            if (hasMorningEntry && isNewWorkspaceWithinRange(
                daymarkEntry.getUserAccount().getCreatedAt(),
                weekStartDateTime,
                weekEndExclusiveDateTime
            )) {
                newWorkspaceActivatedUserIds.add(daymarkEntry.getUserAccountId().getValue());
            }

            GoalCompletionAccumulator goalCompletionAccumulator =
                analyzeGoalCompletion(daymarkEntry.readSection(EDaymarkSectionType.EVENING_GOALS));
            totalTrackedGoals += goalCompletionAccumulator.getTotalGoals();
            completedTrackedGoals += goalCompletionAccumulator.getCompletedGoals();
        }

        Set<Long> weeklyActiveUserIds =
            new HashSet<>(mOperationUsageEventRepository.findDistinctUserAccountIdsWithinExcludingUserRole(
                weekStartDateTime,
                weekEndExclusiveDateTime,
                EXCLUDED_OPERATION_USER_ROLE
            ));
        weeklyActiveUserIds.addAll(weeklyWritingUserIds);

        long weeklyActiveUsers = weeklyActiveUserIds.size();
        long weeklyWritingUsers = weeklyWritingUserIds.size();
        long totalRegisteredUsers = mUserAccountRepository.countExcludingUserRole(EXCLUDED_OPERATION_USER_ROLE);
        long newlyRegisteredUsers = mUserAccountRepository.countCreatedWithinExcludingUserRole(
            weekStartDateTime,
            weekEndExclusiveDateTime,
            EXCLUDED_OPERATION_USER_ROLE
        );
        long newWorkspaceActivatedUsers = newWorkspaceActivatedUserIds.size();

        double averageWritingDaysPerActiveUser = weeklyActiveUsers == 0
            ? 0.0
            : (double) weeklyWritingDays / weeklyActiveUsers;
        double averageEntryCompletionsPerActiveUser = weeklyActiveUsers == 0
            ? 0.0
            : (double) (weeklyMorningEntries + weeklyEveningEntries) / weeklyActiveUsers;
        double planReviewConversionRatePercent = weeklyMorningEntries == 0L
            ? 0.0
            : (double) weeklyPlanReviewCompletedDays * 100.0 / weeklyMorningEntries;
        double newWorkspaceActivationRatePercent = newlyRegisteredUsers == 0L
            ? 0.0
            : (double) newWorkspaceActivatedUsers * 100.0 / newlyRegisteredUsers;
        double goalCompletionRatePercent = totalTrackedGoals == 0
            ? 0.0
            : (double) completedTrackedGoals * 100.0 / totalTrackedGoals;

        return new WeeklyOperationsSummary(
            weekStartDate,
            weekEndDate,
            totalRegisteredUsers,
            newlyRegisteredUsers,
            weeklyActiveUsers,
            weeklyWritingUsers,
            weeklyWritingDays,
            weeklyMorningEntries,
            weeklyEveningEntries,
            weeklyPlanReviewCompletedDays,
            countEvent(EOperationEventType.SIGN_IN_SUCCEEDED, weekStartDateTime, weekEndExclusiveDateTime),
            countEvent(EOperationEventType.SIGN_IN_FAILED, weekStartDateTime, weekEndExclusiveDateTime),
            countEvent(EOperationEventType.WEEKLY_REVIEW_VIEWED, weekStartDateTime, weekEndExclusiveDateTime),
            countEvent(EOperationEventType.RECORD_LIBRARY_VIEWED, weekStartDateTime, weekEndExclusiveDateTime),
            countEvent(EOperationEventType.MARKDOWN_EXPORTED, weekStartDateTime, weekEndExclusiveDateTime),
            countEvent(EOperationEventType.PDF_EXPORT_VIEWED, weekStartDateTime, weekEndExclusiveDateTime),
            countExportingUsers(weekStartDateTime, weekEndExclusiveDateTime),
            newWorkspaceActivatedUsers,
            averageWritingDaysPerActiveUser,
            averageEntryCompletionsPerActiveUser,
            planReviewConversionRatePercent,
            newWorkspaceActivationRatePercent,
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

    private long countEvent(
        EOperationEventType eventType,
        LocalDateTime weekStartDateTime,
        LocalDateTime weekEndExclusiveDateTime
    ) {
        return mOperationUsageEventRepository.countByEventTypeWithinExcludingUserRole(
            eventType,
            weekStartDateTime,
            weekEndExclusiveDateTime,
            EXCLUDED_OPERATION_USER_ROLE
        );
    }

    private long countExportingUsers(LocalDateTime weekStartDateTime, LocalDateTime weekEndExclusiveDateTime) {
        return mOperationUsageEventRepository.countDistinctUserAccountIdsByEventTypesWithinExcludingUserRole(
            EnumSet.of(EOperationEventType.MARKDOWN_EXPORTED, EOperationEventType.PDF_EXPORT_VIEWED),
            weekStartDateTime,
            weekEndExclusiveDateTime,
            EXCLUDED_OPERATION_USER_ROLE
        );
    }

    private static boolean isNewWorkspaceWithinRange(
        LocalDateTime createdAtOrNull,
        LocalDateTime startDateTime,
        LocalDateTime endExclusiveDateTime
    ) {
        return createdAtOrNull != null
            && !createdAtOrNull.isBefore(startDateTime)
            && createdAtOrNull.isBefore(endExclusiveDateTime);
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
