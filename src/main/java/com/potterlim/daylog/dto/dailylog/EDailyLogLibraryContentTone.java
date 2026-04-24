package com.potterlim.daylog.dto.dailylog;

public enum EDailyLogLibraryContentTone {
    FOCUS("library-snapshot-card-focus"),
    RISK("library-snapshot-card-risk"),
    OUTCOME("library-snapshot-card-outcome"),
    IMPROVEMENT("library-snapshot-card-improvement"),
    NEXT("library-snapshot-card-next");

    private final String mCssClassName;

    EDailyLogLibraryContentTone(String cssClassName) {
        mCssClassName = cssClassName;
    }

    public String getCssClassName() {
        return mCssClassName;
    }
}
