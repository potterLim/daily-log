package com.potterlim.daymark.service;

import java.time.LocalDate;

public final class WeeklyOperationsSummary {

    private final LocalDate mWeekStartDate;
    private final LocalDate mWeekEndDate;
    private final long mTotalRegisteredUsers;
    private final long mNewlyRegisteredUsers;
    private final long mWeeklyActiveUsers;
    private final long mWeeklyWritingUsers;
    private final long mWeeklyWritingDays;
    private final long mWeeklyMorningEntries;
    private final long mWeeklyEveningEntries;
    private final long mWeeklyPlanReviewCompletedDays;
    private final long mSignInSucceededCount;
    private final long mSignInFailedCount;
    private final long mWeeklyReviewViewedCount;
    private final long mRecordLibraryViewedCount;
    private final long mMarkdownExportedCount;
    private final long mPdfExportViewedCount;
    private final long mExportingUsers;
    private final long mNewWorkspaceActivatedUsers;
    private final double mAverageWritingDaysPerActiveUser;
    private final double mAverageEntryCompletionsPerActiveUser;
    private final double mPlanReviewConversionRatePercent;
    private final double mNewWorkspaceActivationRatePercent;
    private final double mGoalCompletionRatePercent;

    public WeeklyOperationsSummary(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalRegisteredUsers,
        long newlyRegisteredUsers,
        long weeklyActiveUsers,
        long weeklyWritingUsers,
        long weeklyWritingDays,
        long weeklyMorningEntries,
        long weeklyEveningEntries,
        long weeklyPlanReviewCompletedDays,
        long signInSucceededCount,
        long signInFailedCount,
        long weeklyReviewViewedCount,
        long recordLibraryViewedCount,
        long markdownExportedCount,
        long pdfExportViewedCount,
        long exportingUsers,
        long newWorkspaceActivatedUsers,
        double averageWritingDaysPerActiveUser,
        double averageEntryCompletionsPerActiveUser,
        double planReviewConversionRatePercent,
        double newWorkspaceActivationRatePercent,
        double goalCompletionRatePercent
    ) {
        mWeekStartDate = weekStartDate;
        mWeekEndDate = weekEndDate;
        mTotalRegisteredUsers = totalRegisteredUsers;
        mNewlyRegisteredUsers = newlyRegisteredUsers;
        mWeeklyActiveUsers = weeklyActiveUsers;
        mWeeklyWritingUsers = weeklyWritingUsers;
        mWeeklyWritingDays = weeklyWritingDays;
        mWeeklyMorningEntries = weeklyMorningEntries;
        mWeeklyEveningEntries = weeklyEveningEntries;
        mWeeklyPlanReviewCompletedDays = weeklyPlanReviewCompletedDays;
        mSignInSucceededCount = signInSucceededCount;
        mSignInFailedCount = signInFailedCount;
        mWeeklyReviewViewedCount = weeklyReviewViewedCount;
        mRecordLibraryViewedCount = recordLibraryViewedCount;
        mMarkdownExportedCount = markdownExportedCount;
        mPdfExportViewedCount = pdfExportViewedCount;
        mExportingUsers = exportingUsers;
        mNewWorkspaceActivatedUsers = newWorkspaceActivatedUsers;
        mAverageWritingDaysPerActiveUser = averageWritingDaysPerActiveUser;
        mAverageEntryCompletionsPerActiveUser = averageEntryCompletionsPerActiveUser;
        mPlanReviewConversionRatePercent = planReviewConversionRatePercent;
        mNewWorkspaceActivationRatePercent = newWorkspaceActivationRatePercent;
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

    public long getWeeklyWritingUsers() {
        return mWeeklyWritingUsers;
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

    public long getWeeklyPlanReviewCompletedDays() {
        return mWeeklyPlanReviewCompletedDays;
    }

    public long getSignInSucceededCount() {
        return mSignInSucceededCount;
    }

    public long getSignInFailedCount() {
        return mSignInFailedCount;
    }

    public long getWeeklyReviewViewedCount() {
        return mWeeklyReviewViewedCount;
    }

    public long getRecordLibraryViewedCount() {
        return mRecordLibraryViewedCount;
    }

    public long getMarkdownExportedCount() {
        return mMarkdownExportedCount;
    }

    public long getPdfExportViewedCount() {
        return mPdfExportViewedCount;
    }

    public long getExportCount() {
        return mMarkdownExportedCount + mPdfExportViewedCount;
    }

    public long getExportingUsers() {
        return mExportingUsers;
    }

    public long getNewWorkspaceActivatedUsers() {
        return mNewWorkspaceActivatedUsers;
    }

    public double getAverageWritingDaysPerActiveUser() {
        return mAverageWritingDaysPerActiveUser;
    }

    public double getAverageEntryCompletionsPerActiveUser() {
        return mAverageEntryCompletionsPerActiveUser;
    }

    public double getPlanReviewConversionRatePercent() {
        return mPlanReviewConversionRatePercent;
    }

    public double getNewWorkspaceActivationRatePercent() {
        return mNewWorkspaceActivationRatePercent;
    }

    public double getGoalCompletionRatePercent() {
        return mGoalCompletionRatePercent;
    }
}
