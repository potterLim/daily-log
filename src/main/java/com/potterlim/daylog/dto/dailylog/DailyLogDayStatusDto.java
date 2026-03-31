package com.potterlim.daylog.dto.dailylog;

import java.time.LocalDate;

public final class DailyLogDayStatusDto {

    private final LocalDate mDate;
    private final boolean mHasMorning;
    private final boolean mHasEvening;

    public DailyLogDayStatusDto(LocalDate date, boolean hasMorning, boolean hasEvening) {
        mDate = date;
        mHasMorning = hasMorning;
        mHasEvening = hasEvening;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public boolean hasMorning() {
        return mHasMorning;
    }

    public boolean hasEvening() {
        return mHasEvening;
    }
}
