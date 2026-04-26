package com.potterlim.daymark.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.potterlim.daymark.support.EDaymarkSectionType;
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
    name = "daymark_entry",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_daymark_entry_user_account_id_entry_date",
        columnNames = {"user_account_id", "entry_date"}
    )
)
public class DaymarkEntry {

    private static final List<EDaymarkSectionType> MORNING_SECTION_ORDER = List.of(
        EDaymarkSectionType.GOALS,
        EDaymarkSectionType.FOCUS,
        EDaymarkSectionType.CHALLENGES
    );

    private static final List<EDaymarkSectionType> EVENING_SECTION_ORDER = List.of(
        EDaymarkSectionType.EVENING_GOALS,
        EDaymarkSectionType.ACHIEVEMENTS,
        EDaymarkSectionType.IMPROVEMENTS,
        EDaymarkSectionType.GRATITUDE,
        EDaymarkSectionType.NOTES
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long mId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount mUserAccount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate mEntryDate;

    @Column(name = "has_morning_entry", nullable = false)
    private boolean mHasMorningEntry;

    @Column(name = "has_evening_entry", nullable = false)
    private boolean mHasEveningEntry;

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

    protected DaymarkEntry() {
    }

    private DaymarkEntry(UserAccount userAccount, LocalDate entryDate) {
        mUserAccount = userAccount;
        mEntryDate = entryDate;
        mHasMorningEntry = false;
        mHasEveningEntry = false;
        mGoalsText = "";
        mFocusText = "";
        mChallengesText = "";
        mEveningGoalsText = "";
        mAchievementsText = "";
        mImprovementsText = "";
        mGratitudeText = "";
        mNotesText = "";
    }

    public static DaymarkEntry create(UserAccount userAccount, LocalDate entryDate) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        if (entryDate == null) {
            throw new IllegalArgumentException("entryDate must not be null.");
        }

        return new DaymarkEntry(userAccount, entryDate);
    }

    public LocalDate getEntryDate() {
        return mEntryDate;
    }

    public UserAccountId getUserAccountId() {
        return mUserAccount.getUserAccountId();
    }

    public boolean hasMorningEntry() {
        return hasMorningContent();
    }

    public boolean hasEveningEntry() {
        return hasEveningContent();
    }

    public boolean hasAnyEntryContent() {
        return hasMorningEntry() || hasEveningEntry();
    }

    public String readSection(EDaymarkSectionType daymarkSectionType) {
        return switch (daymarkSectionType) {
            case GOALS -> mGoalsText;
            case FOCUS -> mFocusText;
            case CHALLENGES -> mChallengesText;
            case EVENING_GOALS -> mEveningGoalsText;
            case ACHIEVEMENTS -> mAchievementsText;
            case IMPROVEMENTS -> mImprovementsText;
            case GRATITUDE -> mGratitudeText;
            case NOTES -> mNotesText;
            default -> throw new IllegalStateException("Unexpected daymarkSectionType: " + daymarkSectionType);
        };
    }

    public void writeSection(EDaymarkSectionType daymarkSectionType, String normalizedBody) {
        String safeBody = normalizedBody == null ? "" : normalizedBody;

        switch (daymarkSectionType) {
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
            default -> {
                throw new IllegalStateException("Unexpected daymarkSectionType: " + daymarkSectionType);
            }
        }

        refreshEntryPresenceFlags();
    }

    public String buildMarkdownText() {
        List<String> blocks = new ArrayList<>();

        if (hasMorningEntry()) {
            appendSectionBlocks(blocks, MORNING_SECTION_ORDER);
        }

        if (hasEveningEntry()) {
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

    private void appendSectionBlocks(List<String> blocks, List<EDaymarkSectionType> sectionOrder) {
        for (EDaymarkSectionType daymarkSectionType : sectionOrder) {
            String sectionBlock = buildSectionBlock(daymarkSectionType);
            if (!sectionBlock.isBlank()) {
                blocks.add(sectionBlock);
            }
        }
    }

    private String buildSectionBlock(EDaymarkSectionType daymarkSectionType) {
        String body = readSection(daymarkSectionType).strip();
        if (body.isEmpty()) {
            return "";
        }

        List<String> blockLines = new ArrayList<>();
        blockLines.add(daymarkSectionType.getHeaderText());

        for (String line : splitLines(body)) {
            if (line.isBlank()) {
                continue;
            }

            String normalizedLine = line.stripLeading().replaceFirst("^-\\s*", "").trim();
            blockLines.add("- " + normalizedLine);
        }

        return String.join("\r\n", blockLines);
    }

    private void refreshEntryPresenceFlags() {
        mHasMorningEntry = hasMorningContent();
        mHasEveningEntry = hasEveningContent();
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
