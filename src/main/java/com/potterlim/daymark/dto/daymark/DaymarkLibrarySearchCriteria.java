package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;

public final class DaymarkLibrarySearchCriteria {

    private static final int DEFAULT_LOOKBACK_DAYS = 89;

    private final LocalDate mStartDate;
    private final LocalDate mEndDate;
    private final String mKeywordOrNull;

    private DaymarkLibrarySearchCriteria(LocalDate startDate, LocalDate endDate, String keywordOrNull) {
        mStartDate = startDate;
        mEndDate = endDate;
        mKeywordOrNull = keywordOrNull;
    }

    public static DaymarkLibrarySearchCriteria create(
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

        return new DaymarkLibrarySearchCriteria(
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
