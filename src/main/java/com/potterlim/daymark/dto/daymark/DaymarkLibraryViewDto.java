package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;
import java.util.List;

public final class DaymarkLibraryViewDto {

    private final DaymarkLibrarySearchCriteria mSearchCriteria;
    private final List<DaymarkLibraryItemDto> mItems;
    private final List<DaymarkLibraryItemDto> mTrendItems;
    private final List<DaymarkLibraryCalendarDayDto> mCalendarDays;
    private final LocalDate mCalendarMonthDate;
    private final int mMorningEntryCount;
    private final int mEveningEntryCount;
    private final int mAchievedGoalCount;
    private final int mTotalGoalCount;
    private final int mAverageCompletionPercent;

    public DaymarkLibraryViewDto(
        DaymarkLibrarySearchCriteria searchCriteria,
        List<DaymarkLibraryItemDto> items,
        List<DaymarkLibraryItemDto> trendItems,
        List<DaymarkLibraryCalendarDayDto> calendarDays,
        LocalDate calendarMonthDate,
        int morningEntryCount,
        int eveningEntryCount,
        int achievedGoalCount,
        int totalGoalCount,
        int averageCompletionPercent
    ) {
        mSearchCriteria = searchCriteria;
        mItems = List.copyOf(items);
        mTrendItems = List.copyOf(trendItems);
        mCalendarDays = List.copyOf(calendarDays);
        mCalendarMonthDate = calendarMonthDate;
        mMorningEntryCount = morningEntryCount;
        mEveningEntryCount = eveningEntryCount;
        mAchievedGoalCount = achievedGoalCount;
        mTotalGoalCount = totalGoalCount;
        mAverageCompletionPercent = averageCompletionPercent;
    }

    public DaymarkLibrarySearchCriteria getSearchCriteria() {
        return mSearchCriteria;
    }

    public List<DaymarkLibraryItemDto> getItems() {
        return mItems;
    }

    public List<DaymarkLibraryItemDto> getTrendItems() {
        return mTrendItems;
    }

    public List<DaymarkLibraryCalendarDayDto> getCalendarDays() {
        return mCalendarDays;
    }

    public LocalDate getCalendarMonthDate() {
        return mCalendarMonthDate;
    }

    public int getEntryCount() {
        return mItems.size();
    }

    public int getMorningEntryCount() {
        return mMorningEntryCount;
    }

    public int getEveningEntryCount() {
        return mEveningEntryCount;
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
