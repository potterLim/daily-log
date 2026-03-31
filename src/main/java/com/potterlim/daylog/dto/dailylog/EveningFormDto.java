package com.potterlim.daylog.dto.dailylog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

public final class EveningFormDto {

    @NotNull(message = "날짜가 필요합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate mDate;

    private List<EveningGoalItemDto> mGoals = new ArrayList<>();
    private String mAchievements = "";
    private String mImprovements = "";
    private String mGratitude = "";
    private String mNotes = "";

    public LocalDate getDate() {
        return mDate;
    }

    public void setDate(LocalDate date) {
        mDate = date;
    }

    public List<EveningGoalItemDto> getGoals() {
        return mGoals;
    }

    public void setGoals(List<EveningGoalItemDto> goals) {
        mGoals = goals;
    }

    public String getAchievements() {
        return mAchievements;
    }

    public void setAchievements(String achievements) {
        mAchievements = achievements;
    }

    public String getImprovements() {
        return mImprovements;
    }

    public void setImprovements(String improvements) {
        mImprovements = improvements;
    }

    public String getGratitude() {
        return mGratitude;
    }

    public void setGratitude(String gratitude) {
        mGratitude = gratitude;
    }

    public String getNotes() {
        return mNotes;
    }

    public void setNotes(String notes) {
        mNotes = notes;
    }
}
