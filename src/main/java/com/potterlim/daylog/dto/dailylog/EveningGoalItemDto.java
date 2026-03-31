package com.potterlim.daylog.dto.dailylog;

public final class EveningGoalItemDto {

    private String mText = "";
    private boolean mDone = false;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public boolean isDone() {
        return mDone;
    }

    public void setDone(boolean done) {
        mDone = done;
    }
}
