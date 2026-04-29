package com.potterlim.daymark.dto.daymark;

import jakarta.validation.constraints.Size;

public final class EveningGoalItemDto {

    public static final int TEXT_MAX_LENGTH = 300;

    private String mText = "";
    private boolean mIsDone;

    @Size(max = TEXT_MAX_LENGTH, message = "목표 항목은 300자 이하로 입력해주세요.")
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
