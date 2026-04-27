package com.potterlim.daymark.dto.operations;

import java.time.LocalDate;
import com.potterlim.daymark.entity.WeeklyOperationMetricSnapshot;
import com.potterlim.daymark.service.WeeklyOperationsSummary;

final class OperationsTrendRawPointDto {

    private final LocalDate mWeekStartDate;
    private final LocalDate mWeekEndDate;
    private final long mTotalRegisteredUsers;
    private final long mNewlyRegisteredUsers;
    private final long mWeeklyActiveUsers;
    private final long mWeeklyWritingUsers;
    private final long mWeeklyWritingDays;
    private final long mSignInSucceededCount;
    private final long mRecordLibraryViewedCount;
    private final long mMarkdownExportedCount;
    private final long mPdfExportViewedCount;
    private final long mMailFailureCount;
    private final double mGoalCompletionRatePercent;

    private OperationsTrendRawPointDto(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalRegisteredUsers,
        long newlyRegisteredUsers,
        long weeklyActiveUsers,
        long weeklyWritingUsers,
        long weeklyWritingDays,
        long signInSucceededCount,
        long recordLibraryViewedCount,
        long markdownExportedCount,
        long pdfExportViewedCount,
        long mailFailureCount,
        double goalCompletionRatePercent
    ) {
        mWeekStartDate = weekStartDate;
        mWeekEndDate = weekEndDate;
        mTotalRegisteredUsers = totalRegisteredUsers;
        mNewlyRegisteredUsers = newlyRegisteredUsers;
        mWeeklyActiveUsers = weeklyActiveUsers;
        mWeeklyWritingUsers = weeklyWritingUsers;
        mWeeklyWritingDays = weeklyWritingDays;
        mSignInSucceededCount = signInSucceededCount;
        mRecordLibraryViewedCount = recordLibraryViewedCount;
        mMarkdownExportedCount = markdownExportedCount;
        mPdfExportViewedCount = pdfExportViewedCount;
        mMailFailureCount = mailFailureCount;
        mGoalCompletionRatePercent = goalCompletionRatePercent;
    }

    static OperationsTrendRawPointDto createFromSnapshot(
        WeeklyOperationMetricSnapshot weeklyOperationMetricSnapshot
    ) {
        long mailFailureCount = weeklyOperationMetricSnapshot.getEmailVerificationMailFailedCount()
            + weeklyOperationMetricSnapshot.getPasswordResetMailFailedCount();

        return new OperationsTrendRawPointDto(
            weeklyOperationMetricSnapshot.getWeekStartDate(),
            weeklyOperationMetricSnapshot.getWeekEndDate(),
            weeklyOperationMetricSnapshot.getTotalRegisteredUsers(),
            weeklyOperationMetricSnapshot.getNewlyRegisteredUsers(),
            weeklyOperationMetricSnapshot.getWeeklyActiveUsers(),
            weeklyOperationMetricSnapshot.getWeeklyWritingUsers(),
            weeklyOperationMetricSnapshot.getWeeklyWritingDays(),
            weeklyOperationMetricSnapshot.getSignInSucceededCount(),
            weeklyOperationMetricSnapshot.getRecordLibraryViewedCount(),
            weeklyOperationMetricSnapshot.getMarkdownExportedCount(),
            weeklyOperationMetricSnapshot.getPdfExportViewedCount(),
            mailFailureCount,
            weeklyOperationMetricSnapshot.getGoalCompletionRatePercent()
        );
    }

    static OperationsTrendRawPointDto createFromSummary(WeeklyOperationsSummary weeklyOperationsSummary) {
        long mailFailureCount = weeklyOperationsSummary.getEmailVerificationMailFailedCount()
            + weeklyOperationsSummary.getPasswordResetMailFailedCount();

        return new OperationsTrendRawPointDto(
            weeklyOperationsSummary.getWeekStartDate(),
            weeklyOperationsSummary.getWeekEndDate(),
            weeklyOperationsSummary.getTotalRegisteredUsers(),
            weeklyOperationsSummary.getNewlyRegisteredUsers(),
            weeklyOperationsSummary.getWeeklyActiveUsers(),
            weeklyOperationsSummary.getWeeklyWritingUsers(),
            weeklyOperationsSummary.getWeeklyWritingDays(),
            weeklyOperationsSummary.getSignInSucceededCount(),
            weeklyOperationsSummary.getRecordLibraryViewedCount(),
            weeklyOperationsSummary.getMarkdownExportedCount(),
            weeklyOperationsSummary.getPdfExportViewedCount(),
            mailFailureCount,
            weeklyOperationsSummary.getGoalCompletionRatePercent()
        );
    }

    LocalDate getWeekStartDate() {
        return mWeekStartDate;
    }

    LocalDate getWeekEndDate() {
        return mWeekEndDate;
    }

    long getTotalRegisteredUsers() {
        return mTotalRegisteredUsers;
    }

    long getNewlyRegisteredUsers() {
        return mNewlyRegisteredUsers;
    }

    long getWeeklyActiveUsers() {
        return mWeeklyActiveUsers;
    }

    long getWeeklyWritingUsers() {
        return mWeeklyWritingUsers;
    }

    long getWeeklyWritingDays() {
        return mWeeklyWritingDays;
    }

    long getSignInSucceededCount() {
        return mSignInSucceededCount;
    }

    long getRecordLibraryViewedCount() {
        return mRecordLibraryViewedCount;
    }

    long getMarkdownExportedCount() {
        return mMarkdownExportedCount;
    }

    long getPdfExportViewedCount() {
        return mPdfExportViewedCount;
    }

    long getMailFailureCount() {
        return mMailFailureCount;
    }

    double getGoalCompletionRatePercent() {
        return mGoalCompletionRatePercent;
    }

    long getEngagementCount() {
        return mSignInSucceededCount + mRecordLibraryViewedCount + mMarkdownExportedCount + mPdfExportViewedCount;
    }
}
