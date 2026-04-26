package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;

public final class WeeklyProgressItemDto {

    private final LocalDate mDate;
    private final int mAchieved;
    private final int mTotal;
    private final int mPercent;

    public WeeklyProgressItemDto(LocalDate date, int achieved, int total, int percent) {
        mDate = date;
        mAchieved = achieved;
        mTotal = total;
        mPercent = percent;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public int getAchieved() {
        return mAchieved;
    }

    public int getTotal() {
        return mTotal;
    }

    public int getPercent() {
        return mPercent;
    }
}
