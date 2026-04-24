package com.potterlim.daylog.dto.dailylog;

public final class DailyLogLibraryGoalPreviewDto {

    private final String mText;
    private final boolean mIsDone;

    public DailyLogLibraryGoalPreviewDto(String text, boolean isDone) {
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
