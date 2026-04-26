package com.potterlim.daymark.dto.daymark;

public final class DaymarkLibraryGoalPreviewDto {

    private final String mText;
    private final boolean mIsDone;

    public DaymarkLibraryGoalPreviewDto(String text, boolean isDone) {
        mText = text;
        mIsDone = isDone;
    }

    public String getText() {
        return mText;
    }

    public boolean isDone() {
        return mIsDone;
    }
}
