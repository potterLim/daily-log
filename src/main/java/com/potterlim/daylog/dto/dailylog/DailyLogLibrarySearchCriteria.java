package com.potterlim.daylog.dto.dailylog;

import java.time.LocalDate;

public final class DailyLogLibrarySearchCriteria {

    private static final int DEFAULT_LOOKBACK_DAYS = 89;

    private final LocalDate mStartDate;
    private final LocalDate mEndDate;
    private final String mKeywordOrNull;

    private DailyLogLibrarySearchCriteria(LocalDate startDate, LocalDate endDate, String keywordOrNull) {
        mStartDate = startDate;
        mEndDate = endDate;
        mKeywordOrNull = keywordOrNull;
    }

    public static DailyLogLibrarySearchCriteria create(
        LocalDate startDateOrNull,
        LocalDate endDateOrNull,
        String keywordOrNull,
        LocalDate referenceDate
    ) {
        if (referenceDate == null) {
            throw new IllegalArgumentException("referenceDate must not be null.");
        }

        LocalDate resolvedEndDate = endDateOrNull == null ? referenceDate : endDateOrNull;
        LocalDate resolvedStartDate =
            startDateOrNull == null ? resolvedEndDate.minusDays(DEFAULT_LOOKBACK_DAYS) : startDateOrNull;

        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            LocalDate previousStartDate = resolvedStartDate;
            resolvedStartDate = resolvedEndDate;
            resolvedEndDate = previousStartDate;
        }

        return new DailyLogLibrarySearchCriteria(
            resolvedStartDate,
            resolvedEndDate,
            normalizeKeywordOrNull(keywordOrNull)
        );
    }

    public LocalDate getStartDate() {
        return mStartDate;
    }

    public LocalDate getEndDate() {
        return mEndDate;
    }

    public String getKeywordOrNull() {
        return mKeywordOrNull;
    }

    public boolean hasKeyword() {
        return mKeywordOrNull != null && !mKeywordOrNull.isBlank();
    }

    private static String normalizeKeywordOrNull(String keywordOrNull) {
        if (keywordOrNull == null || keywordOrNull.isBlank()) {
            return null;
        }

        return keywordOrNull.trim();
    }
}
