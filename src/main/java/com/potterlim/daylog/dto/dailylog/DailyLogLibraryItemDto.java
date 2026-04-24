package com.potterlim.daylog.dto.dailylog;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class DailyLogLibraryItemDto {

    private final LocalDate mDate;
    private final boolean mHasMorningLog;
    private final boolean mHasEveningLog;
    private final int mAchievedGoalCount;
    private final int mTotalGoalCount;
    private final int mCompletionPercent;
    private final String mExcerpt;
    private final String mMarkdownText;

    public DailyLogLibraryItemDto(
        LocalDate date,
        boolean hasMorningLog,
        boolean hasEveningLog,
        int achievedGoalCount,
        int totalGoalCount,
        int completionPercent,
        String excerpt,
        String markdownText
    ) {
        mDate = date;
        mHasMorningLog = hasMorningLog;
        mHasEveningLog = hasEveningLog;
        mAchievedGoalCount = achievedGoalCount;
        mTotalGoalCount = totalGoalCount;
        mCompletionPercent = completionPercent;
        mExcerpt = excerpt;
        mMarkdownText = markdownText;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public boolean hasMorningLog() {
        return mHasMorningLog;
    }

    public boolean hasEveningLog() {
        return mHasEveningLog;
    }

    public int getAchievedGoalCount() {
        return mAchievedGoalCount;
    }

    public int getTotalGoalCount() {
        return mTotalGoalCount;
    }

    public int getCompletionPercent() {
        return mCompletionPercent;
    }

    public int getTrendHeightPercent() {
        if (mTotalGoalCount == 0) {
            return 10;
        }

        return Math.max(12, mCompletionPercent);
    }

    public String getExcerpt() {
        return mExcerpt;
    }

    public String getMarkdownText() {
        return mMarkdownText;
    }

    public String getDayLabel() {
        DayOfWeek dayOfWeek = mDate.getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    public String getFlowLabel() {
        if (mHasMorningLog && mHasEveningLog) {
            return "계획과 회고";
        }

        if (mHasMorningLog) {
            return "아침 계획";
        }

        return "저녁 회고";
    }
}
