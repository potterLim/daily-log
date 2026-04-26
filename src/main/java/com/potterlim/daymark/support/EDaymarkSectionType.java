package com.potterlim.daymark.support;

public enum EDaymarkSectionType {
    GOALS("## 오늘의 목표"),
    FOCUS("## 집중 영역"),
    CHALLENGES("## 예상 변수"),
    EVENING_GOALS("## 완료한 목표"),
    ACHIEVEMENTS("## 성과"),
    IMPROVEMENTS("## 개선점"),
    GRATITUDE("## 감사"),
    NOTES("## 내일 메모");

    private final String mHeaderText;

    EDaymarkSectionType(String headerText) {
        mHeaderText = headerText;
    }

    public String getHeaderText() {
        return mHeaderText;
    }
}
