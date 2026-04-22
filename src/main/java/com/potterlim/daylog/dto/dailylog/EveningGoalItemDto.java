package com.potterlim.daylog.dto.dailylog;

public final class EveningGoalItemDto {

    private String mText = "";
    private boolean mIsDone;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public boolean isDone() {
        return mIsDone;
    }

    public void setDone(boolean isDone) {
        mIsDone = isDone;
    }
}
