package com.potterlim.daylog.dto.dailylog;

import java.time.LocalDate;

public final class DailyLogLibraryCalendarDayDto {

    private final LocalDate mDate;
    private final boolean mIsCurrentMonth;
    private final boolean mHasEntry;
    private final int mCompletionPercent;
    private final boolean mIsToday;

    public DailyLogLibraryCalendarDayDto(
        LocalDate date,
        boolean isCurrentMonth,
        boolean hasEntry,
        int completionPercent,
        boolean isToday
    ) {
        mDate = date;
        mIsCurrentMonth = isCurrentMonth;
        mHasEntry = hasEntry;
        mCompletionPercent = completionPercent;
        mIsToday = isToday;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public int getDayOfMonth() {
        return mDate.getDayOfMonth();
    }

    public boolean isCurrentMonth() {
        return mIsCurrentMonth;
    }

    public boolean hasEntry() {
        return mHasEntry;
    }

    public int getCompletionPercent() {
        return mCompletionPercent;
    }

    public int getIntensityLevel() {
        if (!mHasEntry) {
            return 0;
        }

        if (mCompletionPercent >= 80) {
            return 3;
        }

        if (mCompletionPercent >= 40) {
            return 2;
        }

        return 1;
    }

    public boolean isToday() {
        return mIsToday;
    }
}
