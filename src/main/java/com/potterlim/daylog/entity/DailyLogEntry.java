package com.potterlim.daylog.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.potterlim.daylog.support.EDailyLogSectionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "daily_log_entry",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_log_entry_user_account_id_log_date",
        columnNames = {"user_account_id", "log_date"}
    )
)
public class DailyLogEntry {

    private static final List<EDailyLogSectionType> MORNING_SECTION_ORDER = List.of(
        EDailyLogSectionType.GOALS,
        EDailyLogSectionType.FOCUS,
        EDailyLogSectionType.CHALLENGES
    );

    private static final List<EDailyLogSectionType> EVENING_SECTION_ORDER = List.of(
        EDailyLogSectionType.EVENING_GOALS,
        EDailyLogSectionType.ACHIEVEMENTS,
        EDailyLogSectionType.IMPROVEMENTS,
        EDailyLogSectionType.GRATITUDE,
        EDailyLogSectionType.NOTES
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long mId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount mUserAccount;

    @Column(name = "log_date", nullable = false)
    private LocalDate mLogDate;

    @Column(name = "has_morning_log", nullable = false)
    private boolean mHasMorningLog;

    @Column(name = "has_evening_log", nullable = false)
    private boolean mHasEveningLog;

    @Column(name = "goals_text", nullable = false, columnDefinition = "TEXT")
    private String mGoalsText;

    @Column(name = "focus_text", nullable = false, columnDefinition = "TEXT")
    private String mFocusText;

    @Column(name = "challenges_text", nullable = false, columnDefinition = "TEXT")
    private String mChallengesText;

    @Column(name = "evening_goals_text", nullable = false, columnDefinition = "TEXT")
    private String mEveningGoalsText;

    @Column(name = "achievements_text", nullable = false, columnDefinition = "TEXT")
    private String mAchievementsText;

    @Column(name = "improvements_text", nullable = false, columnDefinition = "TEXT")
    private String mImprovementsText;

    @Column(name = "gratitude_text", nullable = false, columnDefinition = "TEXT")
    private String mGratitudeText;

    @Column(name = "notes_text", nullable = false, columnDefinition = "TEXT")
    private String mNotesText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime mCreatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime mUpdatedAt;

    protected DailyLogEntry() {
    }

    private DailyLogEntry(UserAccount userAccount, LocalDate logDate) {
        mUserAccount = userAccount;
        mLogDate = logDate;
        mHasMorningLog = false;
        mHasEveningLog = false;
        mGoalsText = "";
        mFocusText = "";
        mChallengesText = "";
        mEveningGoalsText = "";
        mAchievementsText = "";
        mImprovementsText = "";
        mGratitudeText = "";
        mNotesText = "";
    }

    public static DailyLogEntry create(UserAccount userAccount, LocalDate logDate) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        if (logDate == null) {
            throw new IllegalArgumentException("logDate must not be null.");
        }

        return new DailyLogEntry(userAccount, logDate);
    }

    public LocalDate getLogDate() {
        return mLogDate;
    }

    public UserAccountId getUserAccountId() {
        return mUserAccount.getUserAccountId();
    }

    public boolean hasMorningLog() {
        return hasMorningContent();
    }

    public boolean hasEveningLog() {
        return hasEveningContent();
    }

    public boolean hasAnyLog() {
        return hasMorningLog() || hasEveningLog();
    }

    public String readSection(EDailyLogSectionType dailyLogSectionType) {
        return switch (dailyLogSectionType) {
            case GOALS -> mGoalsText;
            case FOCUS -> mFocusText;
            case CHALLENGES -> mChallengesText;
            case EVENING_GOALS -> mEveningGoalsText;
            case ACHIEVEMENTS -> mAchievementsText;
            case IMPROVEMENTS -> mImprovementsText;
            case GRATITUDE -> mGratitudeText;
            case NOTES -> mNotesText;
        };
    }

    public void writeSection(EDailyLogSectionType dailyLogSectionType, String normalizedBody) {
        String safeBody = normalizedBody == null ? "" : normalizedBody;

        switch (dailyLogSectionType) {
            case GOALS -> {
                mGoalsText = safeBody;
            }
            case FOCUS -> {
                mFocusText = safeBody;
            }
            case CHALLENGES -> {
                mChallengesText = safeBody;
            }
            case EVENING_GOALS -> {
                mEveningGoalsText = safeBody;
            }
            case ACHIEVEMENTS -> {
                mAchievementsText = safeBody;
            }
            case IMPROVEMENTS -> {
                mImprovementsText = safeBody;
            }
            case GRATITUDE -> {
                mGratitudeText = safeBody;
            }
            case NOTES -> {
                mNotesText = safeBody;
            }
        }

        refreshLogPresenceFlags();
    }

    public String buildMarkdownText() {
        List<String> blocks = new ArrayList<>();

        if (hasMorningLog()) {
            appendSectionBlocks(blocks, MORNING_SECTION_ORDER);
        }

        if (hasEveningLog()) {
            appendSectionBlocks(blocks, EVENING_SECTION_ORDER);
        }

        return String.join("\r\n\r\n", blocks).stripTrailing();
    }

    public List<String> readCheckedGoalTexts() {
        List<String> checkedGoalTexts = new ArrayList<>();

        for (String line : splitLines(mEveningGoalsText)) {
            if (line.startsWith("- [x]") || line.startsWith("- [X]")) {
                checkedGoalTexts.add(line.substring(5).trim());
            }
        }

        return checkedGoalTexts;
    }

    @PrePersist
    public void handleBeforePersist() {
        LocalDateTime now = LocalDateTime.now();
        mCreatedAt = now;
        mUpdatedAt = now;
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        mUpdatedAt = LocalDateTime.now();
    }

    private void appendSectionBlocks(List<String> blocks, List<EDailyLogSectionType> sectionOrder) {
        for (EDailyLogSectionType dailyLogSectionType : sectionOrder) {
            String sectionBlock = buildSectionBlock(dailyLogSectionType);
            if (!sectionBlock.isBlank()) {
                blocks.add(sectionBlock);
            }
        }
    }

    private String buildSectionBlock(EDailyLogSectionType dailyLogSectionType) {
        String body = readSection(dailyLogSectionType).strip();
        if (body.isEmpty()) {
            return "";
        }

        List<String> blockLines = new ArrayList<>();
        blockLines.add(dailyLogSectionType.getHeaderText());

        for (String line : splitLines(body)) {
            if (line.isBlank()) {
                continue;
            }

            String normalizedLine = line.stripLeading().replaceFirst("^-\\s*", "").trim();
            blockLines.add("- " + normalizedLine);
        }

        return String.join("\r\n", blockLines);
    }

    private void refreshLogPresenceFlags() {
        mHasMorningLog = hasMorningContent();
        mHasEveningLog = hasEveningContent();
    }

    private boolean hasMorningContent() {
        return hasText(mGoalsText) || hasText(mFocusText) || hasText(mChallengesText);
    }

    private boolean hasEveningContent() {
        return hasText(mEveningGoalsText)
            || hasText(mAchievementsText)
            || hasText(mImprovementsText)
            || hasText(mGratitudeText)
            || hasText(mNotesText);
    }

    private static boolean hasText(String textOrNull) {
        return textOrNull != null && !textOrNull.isBlank();
    }

    private static String[] splitLines(String textOrNull) {
        if (textOrNull == null || textOrNull.isEmpty()) {
            return new String[0];
        }

        return textOrNull.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }
}
