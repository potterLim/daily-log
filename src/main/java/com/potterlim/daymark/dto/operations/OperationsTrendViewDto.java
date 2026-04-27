package com.potterlim.daymark.dto.operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import com.potterlim.daymark.entity.WeeklyOperationMetricSnapshot;
import com.potterlim.daymark.service.WeeklyOperationsSummary;

public final class OperationsTrendViewDto {

    private static final int MAXIMUM_TREND_POINT_COUNT = 12;

    private final List<OperationsTrendPointDto> mTrendPoints;
    private final String mActiveUserLinePoints;
    private final String mWritingUserLinePoints;
    private final String mNewUserLinePoints;
    private final String mGoalCompletionLinePoints;
    private final String mActiveUserDeltaText;
    private final String mWritingUserDeltaText;
    private final String mGoalCompletionDeltaText;
    private final boolean mHasPreviousTrendPoint;

    private OperationsTrendViewDto(
        List<OperationsTrendPointDto> trendPoints,
        String activeUserLinePoints,
        String writingUserLinePoints,
        String newUserLinePoints,
        String goalCompletionLinePoints,
        String activeUserDeltaText,
        String writingUserDeltaText,
        String goalCompletionDeltaText,
        boolean hasPreviousTrendPoint
    ) {
        mTrendPoints = List.copyOf(trendPoints);
        mActiveUserLinePoints = activeUserLinePoints;
        mWritingUserLinePoints = writingUserLinePoints;
        mNewUserLinePoints = newUserLinePoints;
        mGoalCompletionLinePoints = goalCompletionLinePoints;
        mActiveUserDeltaText = activeUserDeltaText;
        mWritingUserDeltaText = writingUserDeltaText;
        mGoalCompletionDeltaText = goalCompletionDeltaText;
        mHasPreviousTrendPoint = hasPreviousTrendPoint;
    }

    public static OperationsTrendViewDto create(
        List<WeeklyOperationMetricSnapshot> recentWeeklySnapshots,
        WeeklyOperationsSummary currentWeeklySummary
    ) {
        if (recentWeeklySnapshots == null) {
            throw new IllegalArgumentException("recentWeeklySnapshots must not be null.");
        }

        if (currentWeeklySummary == null) {
            throw new IllegalArgumentException("currentWeeklySummary must not be null.");
        }

        List<OperationsTrendRawPointDto> rawTrendPoints = createRawTrendPoints(
            recentWeeklySnapshots,
            currentWeeklySummary
        );
        long maximumUserCount = calculateMaximumUserCount(rawTrendPoints);
        long maximumEngagementCount = calculateMaximumEngagementCount(rawTrendPoints);
        List<OperationsTrendPointDto> trendPoints = createTrendPoints(
            rawTrendPoints,
            maximumUserCount,
            maximumEngagementCount
        );

        OperationsTrendPointDto latestTrendPoint = trendPoints.get(trendPoints.size() - 1);
        OperationsTrendPointDto previousTrendPointOrNull =
            trendPoints.size() > 1 ? trendPoints.get(trendPoints.size() - 2) : null;

        return new OperationsTrendViewDto(
            trendPoints,
            buildLinePoints(trendPoints, OperationsTrendPointDto::getActiveUserYAxisCoordinate),
            buildLinePoints(trendPoints, OperationsTrendPointDto::getWritingUserYAxisCoordinate),
            buildLinePoints(trendPoints, OperationsTrendPointDto::getNewUserYAxisCoordinate),
            buildLinePoints(trendPoints, OperationsTrendPointDto::getGoalCompletionYAxisCoordinate),
            formatLongDelta(
                latestTrendPoint.getWeeklyActiveUsers(),
                previousTrendPointOrNull == null ? null : previousTrendPointOrNull.getWeeklyActiveUsers()
            ),
            formatLongDelta(
                latestTrendPoint.getWeeklyWritingUsers(),
                previousTrendPointOrNull == null ? null : previousTrendPointOrNull.getWeeklyWritingUsers()
            ),
            formatPercentPointDelta(
                latestTrendPoint.getGoalCompletionRatePercent(),
                previousTrendPointOrNull == null ? null : previousTrendPointOrNull.getGoalCompletionRatePercent()
            ),
            previousTrendPointOrNull != null
        );
    }

    public List<OperationsTrendPointDto> getTrendPoints() {
        return mTrendPoints;
    }

    public String getActiveUserLinePoints() {
        return mActiveUserLinePoints;
    }

    public String getWritingUserLinePoints() {
        return mWritingUserLinePoints;
    }

    public String getNewUserLinePoints() {
        return mNewUserLinePoints;
    }

    public String getGoalCompletionLinePoints() {
        return mGoalCompletionLinePoints;
    }

    public String getActiveUserDeltaText() {
        return mActiveUserDeltaText;
    }

    public String getWritingUserDeltaText() {
        return mWritingUserDeltaText;
    }

    public String getGoalCompletionDeltaText() {
        return mGoalCompletionDeltaText;
    }

    public boolean hasPreviousTrendPoint() {
        return mHasPreviousTrendPoint;
    }

    private static List<OperationsTrendRawPointDto> createRawTrendPoints(
        List<WeeklyOperationMetricSnapshot> recentWeeklySnapshots,
        WeeklyOperationsSummary currentWeeklySummary
    ) {
        List<OperationsTrendRawPointDto> rawTrendPoints = new ArrayList<>();
        for (WeeklyOperationMetricSnapshot weeklyOperationMetricSnapshot : recentWeeklySnapshots) {
            boolean isCurrentWeekSnapshot = weeklyOperationMetricSnapshot.getWeekStartDate()
                .equals(currentWeeklySummary.getWeekStartDate());

            if (!isCurrentWeekSnapshot) {
                rawTrendPoints.add(
                    OperationsTrendRawPointDto.createFromSnapshot(weeklyOperationMetricSnapshot)
                );
            }
        }

        rawTrendPoints.add(OperationsTrendRawPointDto.createFromSummary(currentWeeklySummary));
        rawTrendPoints.sort(Comparator.comparing(OperationsTrendRawPointDto::getWeekStartDate));

        if (rawTrendPoints.size() <= MAXIMUM_TREND_POINT_COUNT) {
            return rawTrendPoints;
        }

        return new ArrayList<>(rawTrendPoints.subList(
            rawTrendPoints.size() - MAXIMUM_TREND_POINT_COUNT,
            rawTrendPoints.size()
        ));
    }

    private static long calculateMaximumUserCount(List<OperationsTrendRawPointDto> rawTrendPoints) {
        long maximumUserCount = 1L;
        for (OperationsTrendRawPointDto rawTrendPoint : rawTrendPoints) {
            maximumUserCount = Math.max(maximumUserCount, rawTrendPoint.getWeeklyActiveUsers());
            maximumUserCount = Math.max(maximumUserCount, rawTrendPoint.getWeeklyWritingUsers());
            maximumUserCount = Math.max(maximumUserCount, rawTrendPoint.getNewlyRegisteredUsers());
        }

        return maximumUserCount;
    }

    private static long calculateMaximumEngagementCount(List<OperationsTrendRawPointDto> rawTrendPoints) {
        long maximumEngagementCount = 1L;
        for (OperationsTrendRawPointDto rawTrendPoint : rawTrendPoints) {
            maximumEngagementCount = Math.max(maximumEngagementCount, rawTrendPoint.getEngagementCount());
        }

        return maximumEngagementCount;
    }

    private static List<OperationsTrendPointDto> createTrendPoints(
        List<OperationsTrendRawPointDto> rawTrendPoints,
        long maximumUserCount,
        long maximumEngagementCount
    ) {
        List<OperationsTrendPointDto> trendPoints = new ArrayList<>();
        for (int pointIndex = 0; pointIndex < rawTrendPoints.size(); pointIndex += 1) {
            trendPoints.add(new OperationsTrendPointDto(
                rawTrendPoints.get(pointIndex),
                pointIndex,
                rawTrendPoints.size(),
                maximumUserCount,
                maximumEngagementCount
            ));
        }

        return trendPoints;
    }

    private static String buildLinePoints(
        List<OperationsTrendPointDto> trendPoints,
        ToDoubleFunction<OperationsTrendPointDto> yAxisCoordinateReader
    ) {
        List<String> linePoints = new ArrayList<>();
        for (OperationsTrendPointDto trendPoint : trendPoints) {
            linePoints.add(String.format(
                Locale.US,
                "%.1f,%.1f",
                trendPoint.getXAxisCoordinate(),
                yAxisCoordinateReader.applyAsDouble(trendPoint)
            ));
        }

        return String.join(" ", linePoints);
    }

    private static String formatLongDelta(long currentValue, Long previousValueOrNull) {
        if (previousValueOrNull == null) {
            return "-";
        }

        long delta = currentValue - previousValueOrNull;
        return delta >= 0L ? "+" + delta : String.valueOf(delta);
    }

    private static String formatPercentPointDelta(double currentValue, Double previousValueOrNull) {
        if (previousValueOrNull == null) {
            return "-";
        }

        double delta = currentValue - previousValueOrNull;
        return String.format(Locale.US, "%+.1fp", delta);
    }
}
