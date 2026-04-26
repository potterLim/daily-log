package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

public final class MorningFormDto {

    @NotNull(message = "날짜가 필요합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate mDate;

    private String mGoals = "";
    private String mFocus = "";
    private String mChallenges = "";

    public LocalDate getDate() {
        return mDate;
    }

    public void setDate(LocalDate date) {
        mDate = date;
    }

    public String getGoals() {
        return mGoals;
    }

    public void setGoals(String goals) {
        mGoals = goals;
    }

    public String getFocus() {
        return mFocus;
    }

    public void setFocus(String focus) {
        mFocus = focus;
    }

    public String getChallenges() {
        return mChallenges;
    }

    public void setChallenges(String challenges) {
        mChallenges = challenges;
    }
}
