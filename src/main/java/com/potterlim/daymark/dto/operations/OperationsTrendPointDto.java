package com.potterlim.daymark.dto.operations;

import java.time.LocalDate;

public final class OperationsTrendPointDto {

    private static final double CHART_X_START = 28.0;
    private static final double CHART_X_END = 652.0;
    private static final double CHART_Y_TOP = 18.0;
    private static final double CHART_Y_BOTTOM = 188.0;
    private static final int MINIMUM_VISIBLE_PERCENT = 6;

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
    private final double mGoalCompletionRatePercent;
    private final int mActiveUserBarPercent;
    private final int mWritingUserBarPercent;
    private final int mNewUserBarPercent;
    private final int mEngagementBarPercent;
    private final double mXAxisCoordinate;
    private final double mActiveUserYAxisCoordinate;
    private final double mWritingUserYAxisCoordinate;
    private final double mNewUserYAxisCoordinate;
    private final double mGoalCompletionYAxisCoordinate;

    OperationsTrendPointDto(
        OperationsTrendRawPointDto operationsTrendRawPointDto,
        int pointIndex,
        int pointCount,
        long maximumUserCount,
        long maximumEngagementCount
    ) {
        mWeekStartDate = operationsTrendRawPointDto.getWeekStartDate();
        mWeekEndDate = operationsTrendRawPointDto.getWeekEndDate();
        mTotalRegisteredUsers = operationsTrendRawPointDto.getTotalRegisteredUsers();
        mNewlyRegisteredUsers = operationsTrendRawPointDto.getNewlyRegisteredUsers();
        mWeeklyActiveUsers = operationsTrendRawPointDto.getWeeklyActiveUsers();
        mWeeklyWritingUsers = operationsTrendRawPointDto.getWeeklyWritingUsers();
        mWeeklyWritingDays = operationsTrendRawPointDto.getWeeklyWritingDays();
        mSignInSucceededCount = operationsTrendRawPointDto.getSignInSucceededCount();
        mRecordLibraryViewedCount = operationsTrendRawPointDto.getRecordLibraryViewedCount();
        mMarkdownExportedCount = operationsTrendRawPointDto.getMarkdownExportedCount();
        mPdfExportViewedCount = operationsTrendRawPointDto.getPdfExportViewedCount();
        mGoalCompletionRatePercent = operationsTrendRawPointDto.getGoalCompletionRatePercent();
        mXAxisCoordinate = calculateXAxisCoordinate(pointIndex, pointCount);
        mActiveUserYAxisCoordinate = calculateYAxisCoordinate(mWeeklyActiveUsers, maximumUserCount);
        mWritingUserYAxisCoordinate = calculateYAxisCoordinate(mWeeklyWritingUsers, maximumUserCount);
        mNewUserYAxisCoordinate = calculateYAxisCoordinate(mNewlyRegisteredUsers, maximumUserCount);
        mGoalCompletionYAxisCoordinate = calculateYAxisCoordinate(mGoalCompletionRatePercent, 100.0);
        mActiveUserBarPercent = calculateBarPercent(mWeeklyActiveUsers, maximumUserCount);
        mWritingUserBarPercent = calculateBarPercent(mWeeklyWritingUsers, maximumUserCount);
        mNewUserBarPercent = calculateBarPercent(mNewlyRegisteredUsers, maximumUserCount);
        mEngagementBarPercent = calculateBarPercent(getEngagementCount(), maximumEngagementCount);
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

    public long getSignInSucceededCount() {
        return mSignInSucceededCount;
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

    public double getGoalCompletionRatePercent() {
        return mGoalCompletionRatePercent;
    }

    public long getExportCount() {
        return mMarkdownExportedCount + mPdfExportViewedCount;
    }

    public long getEngagementCount() {
        return mSignInSucceededCount + mRecordLibraryViewedCount + getExportCount();
    }

    public int getActiveUserBarPercent() {
        return mActiveUserBarPercent;
    }

    public int getWritingUserBarPercent() {
        return mWritingUserBarPercent;
    }

    public int getNewUserBarPercent() {
        return mNewUserBarPercent;
    }

    public int getEngagementBarPercent() {
        return mEngagementBarPercent;
    }

    public double getXAxisCoordinate() {
        return mXAxisCoordinate;
    }

    public double getActiveUserYAxisCoordinate() {
        return mActiveUserYAxisCoordinate;
    }

    public double getWritingUserYAxisCoordinate() {
        return mWritingUserYAxisCoordinate;
    }

    public double getNewUserYAxisCoordinate() {
        return mNewUserYAxisCoordinate;
    }

    public double getGoalCompletionYAxisCoordinate() {
        return mGoalCompletionYAxisCoordinate;
    }

    private static double calculateXAxisCoordinate(int pointIndex, int pointCount) {
        if (pointCount <= 1) {
            return (CHART_X_START + CHART_X_END) / 2.0;
        }

        double chartWidth = CHART_X_END - CHART_X_START;
        return CHART_X_START + chartWidth * pointIndex / (pointCount - 1);
    }

    private static double calculateYAxisCoordinate(double metricValue, double maximumMetricValue) {
        if (maximumMetricValue <= 0.0) {
            return CHART_Y_BOTTOM;
        }

        double safeRatio = Math.max(0.0, Math.min(1.0, metricValue / maximumMetricValue));
        return CHART_Y_BOTTOM - (CHART_Y_BOTTOM - CHART_Y_TOP) * safeRatio;
    }

    private static int calculateBarPercent(long metricValue, long maximumMetricValue) {
        if (metricValue <= 0L || maximumMetricValue <= 0L) {
            return 0;
        }

        int barPercent = (int) Math.round(metricValue * 100.0 / maximumMetricValue);
        return Math.max(MINIMUM_VISIBLE_PERCENT, barPercent);
    }
}
