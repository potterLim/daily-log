package com.potterlim.daylog.service;

import java.time.LocalDate;

public final class WeeklyOperationsSummary {

    private final LocalDate mWeekStartDate;
    private final LocalDate mWeekEndDate;
    private final long mTotalRegisteredUsers;
    private final long mNewlyRegisteredUsers;
    private final long mWeeklyActiveUsers;
    private final long mWeeklyWritingDays;
    private final long mWeeklyMorningLogs;
    private final long mWeeklyEveningLogs;
    private final double mAverageWritingDaysPerActiveUser;
    private final double mAverageLogCompletionsPerActiveUser;
    private final double mGoalCompletionRatePercent;

    public WeeklyOperationsSummary(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalRegisteredUsers,
        long newlyRegisteredUsers,
        long weeklyActiveUsers,
        long weeklyWritingDays,
        long weeklyMorningLogs,
        long weeklyEveningLogs,
        double averageWritingDaysPerActiveUser,
        double averageLogCompletionsPerActiveUser,
        double goalCompletionRatePercent
    ) {
        mWeekStartDate = weekStartDate;
        mWeekEndDate = weekEndDate;
        mTotalRegisteredUsers = totalRegisteredUsers;
        mNewlyRegisteredUsers = newlyRegisteredUsers;
        mWeeklyActiveUsers = weeklyActiveUsers;
        mWeeklyWritingDays = weeklyWritingDays;
        mWeeklyMorningLogs = weeklyMorningLogs;
        mWeeklyEveningLogs = weeklyEveningLogs;
        mAverageWritingDaysPerActiveUser = averageWritingDaysPerActiveUser;
        mAverageLogCompletionsPerActiveUser = averageLogCompletionsPerActiveUser;
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

    public long getWeeklyMorningLogs() {
        return mWeeklyMorningLogs;
    }

    public long getWeeklyEveningLogs() {
        return mWeeklyEveningLogs;
    }

    public double getAverageWritingDaysPerActiveUser() {
        return mAverageWritingDaysPerActiveUser;
    }

    public double getAverageLogCompletionsPerActiveUser() {
        return mAverageLogCompletionsPerActiveUser;
    }

    public double getGoalCompletionRatePercent() {
        return mGoalCompletionRatePercent;
    }
}
