package com.potterlim.daymark.dto.daymark;

public enum EDaymarkLibraryContentTone {
    FOCUS("library-snapshot-card-focus"),
    RISK("library-snapshot-card-risk"),
    OUTCOME("library-snapshot-card-outcome"),
    IMPROVEMENT("library-snapshot-card-improvement"),
    NEXT("library-snapshot-card-next");

    private final String mCssClassName;

    EDaymarkLibraryContentTone(String cssClassName) {
        mCssClassName = cssClassName;
    }

    public String getCssClassName() {
        return mCssClassName;
    }
}
