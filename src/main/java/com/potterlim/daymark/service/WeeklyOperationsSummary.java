package com.potterlim.daymark.service;

import java.time.LocalDate;

public final class WeeklyOperationsSummary {

    private final LocalDate mWeekStartDate;
    private final LocalDate mWeekEndDate;
    private final long mTotalRegisteredUsers;
    private final long mNewlyRegisteredUsers;
    private final long mWeeklyActiveUsers;
    private final long mWeeklyWritingDays;
    private final long mWeeklyMorningEntries;
    private final long mWeeklyEveningEntries;
    private final double mAverageWritingDaysPerActiveUser;
    private final double mAverageEntryCompletionsPerActiveUser;
    private final double mGoalCompletionRatePercent;

    public WeeklyOperationsSummary(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalRegisteredUsers,
        long newlyRegisteredUsers,
        long weeklyActiveUsers,
        long weeklyWritingDays,
        long weeklyMorningEntries,
        long weeklyEveningEntries,
        double averageWritingDaysPerActiveUser,
        double averageEntryCompletionsPerActiveUser,
        double goalCompletionRatePercent
    ) {
        mWeekStartDate = weekStartDate;
        mWeekEndDate = weekEndDate;
        mTotalRegisteredUsers = totalRegisteredUsers;
        mNewlyRegisteredUsers = newlyRegisteredUsers;
        mWeeklyActiveUsers = weeklyActiveUsers;
        mWeeklyWritingDays = weeklyWritingDays;
        mWeeklyMorningEntries = weeklyMorningEntries;
        mWeeklyEveningEntries = weeklyEveningEntries;
        mAverageWritingDaysPerActiveUser = averageWritingDaysPerActiveUser;
        mAverageEntryCompletionsPerActiveUser = averageEntryCompletionsPerActiveUser;
        mGoalCompletionRatePercent = goalCompletionRatePercent;
    }

    public LocalDate getWeekStartDate() {
        return mWeekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return mWeekEndDate;
    }

    public long getTotalRegisteredUsers() {
        return mTotalRegisteredUsers;
    }

    public long getNewlyRegisteredUsers() {
        return mNewlyRegisteredUsers;
    }

    public long getWeeklyActiveUsers() {
        return mWeeklyActiveUsers;
    }

    public long getWeeklyWritingDays() {
        return mWeeklyWritingDays;
    }

    public long getWeeklyMorningEntries() {
        return mWeeklyMorningEntries;
    }

    public long getWeeklyEveningEntries() {
        return mWeeklyEveningEntries;
    }

    public double getAverageWritingDaysPerActiveUser() {
        return mAverageWritingDaysPerActiveUser;
    }

    public double getAverageEntryCompletionsPerActiveUser() {
        return mAverageEntryCompletionsPerActiveUser;
    }

    public double getGoalCompletionRatePercent() {
        return mGoalCompletionRatePercent;
    }
}
