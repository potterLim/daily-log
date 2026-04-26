package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

public final class EveningFormDto {

    private LocalDate mDate;

    private List<EveningGoalItemDto> mGoals = new ArrayList<>();
    private String mAchievements = "";
    private String mImprovements = "";
    private String mGratitude = "";
    private String mNotes = "";

    @NotNull(message = "날짜가 필요합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
