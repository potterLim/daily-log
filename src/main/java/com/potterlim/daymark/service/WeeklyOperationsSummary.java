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
    private final long mSignInSucceededCount;
    private final long mSignInFailedCount;
    private final long mEmailVerificationMailSentCount;
    private final long mEmailVerificationMailFailedCount;
    private final long mEmailVerifiedCount;
    private final long mPasswordResetRequestedCount;
    private final long mPasswordResetMailSentCount;
    private final long mPasswordResetMailFailedCount;
    private final long mPasswordResetCompletedCount;
    private final long mRecordLibraryViewedCount;
    private final long mMarkdownExportedCount;
    private final long mPdfExportViewedCount;
    private final double mAverageWritingDaysPerActiveUser;
    private final double mAverageEntryCompletionsPerActiveUser;
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
        long signInSucceededCount,
        long signInFailedCount,
        long emailVerificationMailSentCount,
        long emailVerificationMailFailedCount,
        long emailVerifiedCount,
        long passwordResetRequestedCount,
        long passwordResetMailSentCount,
        long passwordResetMailFailedCount,
        long passwordResetCompletedCount,
        long recordLibraryViewedCount,
        long markdownExportedCount,
        long pdfExportViewedCount,
        double averageWritingDaysPerActiveUser,
        double averageEntryCompletionsPerActiveUser,
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
        mSignInSucceededCount = signInSucceededCount;
        mSignInFailedCount = signInFailedCount;
        mEmailVerificationMailSentCount = emailVerificationMailSentCount;
        mEmailVerificationMailFailedCount = emailVerificationMailFailedCount;
        mEmailVerifiedCount = emailVerifiedCount;
        mPasswordResetRequestedCount = passwordResetRequestedCount;
        mPasswordResetMailSentCount = passwordResetMailSentCount;
        mPasswordResetMailFailedCount = passwordResetMailFailedCount;
        mPasswordResetCompletedCount = passwordResetCompletedCount;
        mRecordLibraryViewedCount = recordLibraryViewedCount;
        mMarkdownExportedCount = markdownExportedCount;
        mPdfExportViewedCount = pdfExportViewedCount;
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

    public long getSignInSucceededCount() {
        return mSignInSucceededCount;
    }

    public long getSignInFailedCount() {
        return mSignInFailedCount;
    }

    public long getEmailVerificationMailSentCount() {
        return mEmailVerificationMailSentCount;
    }

    public long getEmailVerificationMailFailedCount() {
        return mEmailVerificationMailFailedCount;
    }

    public long getEmailVerifiedCount() {
        return mEmailVerifiedCount;
    }

    public long getPasswordResetRequestedCount() {
        return mPasswordResetRequestedCount;
    }

    public long getPasswordResetMailSentCount() {
        return mPasswordResetMailSentCount;
    }

    public long getPasswordResetMailFailedCount() {
        return mPasswordResetMailFailedCount;
    }

    public long getPasswordResetCompletedCount() {
        return mPasswordResetCompletedCount;
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
