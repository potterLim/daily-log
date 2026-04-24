package com.potterlim.daylog.dto.dailylog;

import java.time.LocalDate;
import java.util.List;

public final class DailyLogLibraryViewDto {

    private final DailyLogLibrarySearchCriteria mSearchCriteria;
    private final List<DailyLogLibraryItemDto> mItems;
    private final List<DailyLogLibraryItemDto> mTrendItems;
    private final List<DailyLogLibraryCalendarDayDto> mCalendarDays;
    private final LocalDate mCalendarMonthDate;
    private final int mMorningLogCount;
    private final int mEveningLogCount;
    private final int mAchievedGoalCount;
    private final int mTotalGoalCount;
    private final int mAverageCompletionPercent;

    public DailyLogLibraryViewDto(
        DailyLogLibrarySearchCriteria searchCriteria,
        List<DailyLogLibraryItemDto> items,
        List<DailyLogLibraryItemDto> trendItems,
        List<DailyLogLibraryCalendarDayDto> calendarDays,
        LocalDate calendarMonthDate,
        int morningLogCount,
        int eveningLogCount,
        int achievedGoalCount,
        int totalGoalCount,
        int averageCompletionPercent
    ) {
        mSearchCriteria = searchCriteria;
        mItems = List.copyOf(items);
        mTrendItems = List.copyOf(trendItems);
        mCalendarDays = List.copyOf(calendarDays);
        mCalendarMonthDate = calendarMonthDate;
        mMorningLogCount = morningLogCount;
        mEveningLogCount = eveningLogCount;
        mAchievedGoalCount = achievedGoalCount;
        mTotalGoalCount = totalGoalCount;
        mAverageCompletionPercent = averageCompletionPercent;
    }

    public DailyLogLibrarySearchCriteria getSearchCriteria() {
        return mSearchCriteria;
    }

    public List<DailyLogLibraryItemDto> getItems() {
        return mItems;
    }

    public List<DailyLogLibraryItemDto> getTrendItems() {
        return mTrendItems;
    }

    public List<DailyLogLibraryCalendarDayDto> getCalendarDays() {
        return mCalendarDays;
    }

    public LocalDate getCalendarMonthDate() {
        return mCalendarMonthDate;
    }

    public int getEntryCount() {
        return mItems.size();
    }

    public int getMorningLogCount() {
        return mMorningLogCount;
    }

    public int getEveningLogCount() {
        return mEveningLogCount;
    }

    public int getAchievedGoalCount() {
        return mAchievedGoalCount;
    }

    public int getTotalGoalCount() {
        return mTotalGoalCount;
    }

    public int getAverageCompletionPercent() {
        return mAverageCompletionPercent;
    }

    public boolean hasEntries() {
        return !mItems.isEmpty();
    }
}
