package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public final class EveningFormDto {

    public static final int GOALS_MAX_SIZE = 30;
    public static final int ACHIEVEMENTS_MAX_LENGTH = 2_000;
    public static final int IMPROVEMENTS_MAX_LENGTH = 1_500;
    public static final int GRATITUDE_MAX_LENGTH = 1_000;
    public static final int NOTES_MAX_LENGTH = 2_000;
    public static final int TOTAL_BODY_MAX_LENGTH = 8_000;

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

    @Valid
    @Size(max = GOALS_MAX_SIZE, message = "목표 체크는 30개 이하로 저장할 수 있습니다.")
    public List<EveningGoalItemDto> getGoals() {
        return mGoals;
    }

    public void setGoals(List<EveningGoalItemDto> goals) {
        mGoals = goals;
    }

    @Size(max = ACHIEVEMENTS_MAX_LENGTH, message = "성과는 2,000자 이하로 입력해주세요.")
    public String getAchievements() {
        return mAchievements;
    }

    public void setAchievements(String achievements) {
        mAchievements = achievements;
    }

    @Size(max = IMPROVEMENTS_MAX_LENGTH, message = "개선점은 1,500자 이하로 입력해주세요.")
    public String getImprovements() {
        return mImprovements;
    }

    public void setImprovements(String improvements) {
        mImprovements = improvements;
    }

    @Size(max = GRATITUDE_MAX_LENGTH, message = "감사는 1,000자 이하로 입력해주세요.")
    public String getGratitude() {
        return mGratitude;
    }

    public void setGratitude(String gratitude) {
        mGratitude = gratitude;
    }

    @Size(max = NOTES_MAX_LENGTH, message = "내일 메모는 2,000자 이하로 입력해주세요.")
    public String getNotes() {
        return mNotes;
    }

    public void setNotes(String notes) {
        mNotes = notes;
    }

    public boolean isWithinTotalBodyLimit() {
        int totalBodyLength = calculateGoalTextLength(mGoals)
            + calculateLength(mAchievements)
            + calculateLength(mImprovements)
            + calculateLength(mGratitude)
            + calculateLength(mNotes);
        return totalBodyLength <= TOTAL_BODY_MAX_LENGTH;
    }

    private static int calculateGoalTextLength(List<EveningGoalItemDto> goalsOrNull) {
        if (goalsOrNull == null || goalsOrNull.isEmpty()) {
            return 0;
        }

        int totalLength = 0;
        for (EveningGoalItemDto eveningGoalItemDto : goalsOrNull) {
            if (eveningGoalItemDto == null) {
                continue;
            }

            totalLength += calculateLength(eveningGoalItemDto.getText());
        }

        return totalLength;
    }

    private static int calculateLength(String textOrNull) {
        return textOrNull == null ? 0 : textOrNull.length();
    }
}
