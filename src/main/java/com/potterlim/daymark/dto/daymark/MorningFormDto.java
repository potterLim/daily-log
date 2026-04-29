package com.potterlim.daymark.dto.daymark;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public final class MorningFormDto {

    public static final int GOALS_MAX_LENGTH = 2_000;
    public static final int GOAL_MAX_COUNT = 30;
    public static final int GOAL_LINE_MAX_LENGTH = 300;
    public static final int FOCUS_MAX_LENGTH = 1_200;
    public static final int CHALLENGES_MAX_LENGTH = 1_500;
    public static final int TOTAL_BODY_MAX_LENGTH = 8_000;

    private LocalDate mDate;

    private String mGoals = "";
    private String mFocus = "";
    private String mChallenges = "";

    @NotNull(message = "날짜가 필요합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    public LocalDate getDate() {
        return mDate;
    }

    public void setDate(LocalDate date) {
        mDate = date;
    }

    @Size(max = GOALS_MAX_LENGTH, message = "목표는 2,000자 이하로 입력해주세요.")
    public String getGoals() {
        return mGoals;
    }

    public void setGoals(String goals) {
        mGoals = goals;
    }

    @Size(max = FOCUS_MAX_LENGTH, message = "집중 영역은 1,200자 이하로 입력해주세요.")
    public String getFocus() {
        return mFocus;
    }

    public void setFocus(String focus) {
        mFocus = focus;
    }

    @Size(max = CHALLENGES_MAX_LENGTH, message = "예상 변수는 1,500자 이하로 입력해주세요.")
    public String getChallenges() {
        return mChallenges;
    }

    public void setChallenges(String challenges) {
        mChallenges = challenges;
    }

    public boolean isWithinTotalBodyLimit() {
        int totalBodyLength = calculateLength(mGoals) + calculateLength(mFocus) + calculateLength(mChallenges);
        return totalBodyLength <= TOTAL_BODY_MAX_LENGTH;
    }

    public boolean hasValidGoalLineCount() {
        return splitGoalLines().size() <= GOAL_MAX_COUNT;
    }

    public boolean hasValidGoalLineLengths() {
        for (String goalLine : splitGoalLines()) {
            if (goalLine.length() > GOAL_LINE_MAX_LENGTH) {
                return false;
            }
        }

        return true;
    }

    private List<String> splitGoalLines() {
        if (mGoals == null || mGoals.isBlank()) {
            return List.of();
        }

        return mGoals.lines()
            .map(String::trim)
            .filter(goalLine -> !goalLine.isEmpty())
            .toList();
    }

    private static int calculateLength(String textOrNull) {
        return textOrNull == null ? 0 : textOrNull.length();
    }
}
