package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;

public final class DaymarkDayStatusDto {

    private final LocalDate mDate;
    private final boolean mHasMorningEntry;
    private final boolean mHasEveningEntry;

    public DaymarkDayStatusDto(LocalDate date, boolean hasMorningEntry, boolean hasEveningEntry) {
        mDate = date;
        mHasMorningEntry = hasMorningEntry;
        mHasEveningEntry = hasEveningEntry;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public boolean hasMorningEntry() {
        return mHasMorningEntry;
    }

    public boolean hasEveningEntry() {
        return mHasEveningEntry;
    }
}
