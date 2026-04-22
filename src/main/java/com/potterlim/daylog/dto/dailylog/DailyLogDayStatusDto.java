package com.potterlim.daylog.dto.dailylog;

import java.time.LocalDate;

public final class DailyLogDayStatusDto {

    private final LocalDate mDate;
    private final boolean mHasMorningLog;
    private final boolean mHasEveningLog;

    public DailyLogDayStatusDto(LocalDate date, boolean hasMorningLog, boolean hasEveningLog) {
        mDate = date;
        mHasMorningLog = hasMorningLog;
        mHasEveningLog = hasEveningLog;
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
}
