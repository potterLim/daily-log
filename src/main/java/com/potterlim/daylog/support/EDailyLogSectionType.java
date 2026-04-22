package com.potterlim.daylog.support;

public enum EDailyLogSectionType {
    GOALS("## 🚀 Today's Goals"),
    FOCUS("## 🎯 Focus Areas"),
    CHALLENGES("## ⚙️ Challenges & Strategies"),
    EVENING_GOALS("## ✅ Goals Checked"),
    ACHIEVEMENTS("## 🏆 Achievements"),
    IMPROVEMENTS("## 🔧 Improvements"),
    GRATITUDE("## 💛 Gratitude"),
    NOTES("## 📌 Notes for Tomorrow");

    private final String mHeaderText;

    EDailyLogSectionType(String headerText) {
        mHeaderText = headerText;
    }

    public String getHeaderText() {
        return mHeaderText;
    }
}
