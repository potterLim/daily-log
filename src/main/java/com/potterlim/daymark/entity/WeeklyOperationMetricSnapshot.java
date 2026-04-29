package com.potterlim.daymark.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.potterlim.daymark.service.WeeklyOperationsSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "weekly_operation_metric_snapshot",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_weekly_operation_metric_snapshot_week",
        columnNames = {"week_start_date", "week_end_date"}
    )
)
public class WeeklyOperationMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long mId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate mWeekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate mWeekEndDate;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime mGeneratedAt;

    @Column(name = "total_registered_users", nullable = false)
    private long mTotalRegisteredUsers;

    @Column(name = "newly_registered_users", nullable = false)
    private long mNewlyRegisteredUsers;

    @Column(name = "weekly_active_users", nullable = false)
    private long mWeeklyActiveUsers;

    @Column(name = "weekly_writing_users", nullable = false)
    private long mWeeklyWritingUsers;

    @Column(name = "weekly_writing_days", nullable = false)
    private long mWeeklyWritingDays;

    @Column(name = "weekly_morning_entries", nullable = false)
    private long mWeeklyMorningEntries;

    @Column(name = "weekly_evening_entries", nullable = false)
    private long mWeeklyEveningEntries;

    @Column(name = "sign_in_succeeded_count", nullable = false)
    private long mSignInSucceededCount;

    @Column(name = "sign_in_failed_count", nullable = false)
    private long mSignInFailedCount;

    @Column(name = "record_library_viewed_count", nullable = false)
    private long mRecordLibraryViewedCount;

    @Column(name = "markdown_exported_count", nullable = false)
    private long mMarkdownExportedCount;

    @Column(name = "pdf_export_viewed_count", nullable = false)
    private long mPdfExportViewedCount;

    @Column(name = "average_writing_days_per_active_user", nullable = false)
    private double mAverageWritingDaysPerActiveUser;

    @Column(name = "average_entry_completions_per_active_user", nullable = false)
    private double mAverageEntryCompletionsPerActiveUser;

    @Column(name = "goal_completion_rate_percent", nullable = false)
    private double mGoalCompletionRatePercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime mCreatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime mUpdatedAt;

    protected WeeklyOperationMetricSnapshot() {
    }

    private WeeklyOperationMetricSnapshot(LocalDate weekStartDate, LocalDate weekEndDate) {
        mWeekStartDate = weekStartDate;
        mWeekEndDate = weekEndDate;
    }

    public static WeeklyOperationMetricSnapshot create(LocalDate weekStartDate, LocalDate weekEndDate) {
        if (weekStartDate == null) {
            throw new IllegalArgumentException("weekStartDate must not be null.");
        }

        if (weekEndDate == null) {
            throw new IllegalArgumentException("weekEndDate must not be null.");
        }

        return new WeeklyOperationMetricSnapshot(weekStartDate, weekEndDate);
    }

    public void updateFrom(WeeklyOperationsSummary weeklyOperationsSummary, LocalDateTime generatedAt) {
        if (weeklyOperationsSummary == null) {
            throw new IllegalArgumentException("weeklyOperationsSummary must not be null.");
        }

        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt must not be null.");
        }

        mWeekStartDate = weeklyOperationsSummary.getWeekStartDate();
        mWeekEndDate = weeklyOperationsSummary.getWeekEndDate();
        mGeneratedAt = generatedAt;
        mTotalRegisteredUsers = weeklyOperationsSummary.getTotalRegisteredUsers();
        mNewlyRegisteredUsers = weeklyOperationsSummary.getNewlyRegisteredUsers();
        mWeeklyActiveUsers = weeklyOperationsSummary.getWeeklyActiveUsers();
        mWeeklyWritingUsers = weeklyOperationsSummary.getWeeklyWritingUsers();
        mWeeklyWritingDays = weeklyOperationsSummary.getWeeklyWritingDays();
        mWeeklyMorningEntries = weeklyOperationsSummary.getWeeklyMorningEntries();
        mWeeklyEveningEntries = weeklyOperationsSummary.getWeeklyEveningEntries();
        mSignInSucceededCount = weeklyOperationsSummary.getSignInSucceededCount();
        mSignInFailedCount = weeklyOperationsSummary.getSignInFailedCount();
        mRecordLibraryViewedCount = weeklyOperationsSummary.getRecordLibraryViewedCount();
        mMarkdownExportedCount = weeklyOperationsSummary.getMarkdownExportedCount();
        mPdfExportViewedCount = weeklyOperationsSummary.getPdfExportViewedCount();
        mAverageWritingDaysPerActiveUser = weeklyOperationsSummary.getAverageWritingDaysPerActiveUser();
        mAverageEntryCompletionsPerActiveUser = weeklyOperationsSummary.getAverageEntryCompletionsPerActiveUser();
        mGoalCompletionRatePercent = weeklyOperationsSummary.getGoalCompletionRatePercent();
    }

    public LocalDate getWeekStartDate() {
        return mWeekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return mWeekEndDate;
    }

    public LocalDateTime getGeneratedAt() {
        return mGeneratedAt;
    }

    public long getTotalRegisteredUsers() {
        return mTotalRegisteredUsers;
    }

    public long getNewlyRegisteredUsers() {
        return mNewlyRegisteredUsers;
    }

    public long getWeeklyActiveUsers() {
        return mWeeklyActiveUsers;
    }

    public long getWeeklyWritingUsers() {
        return mWeeklyWritingUsers;
    }

    public long getWeeklyWritingDays() {
        return mWeeklyWritingDays;
    }

    public long getWeeklyMorningEntries() {
        return mWeeklyMorningEntries;
    }

    public long getWeeklyEveningEntries() {
        return mWeeklyEveningEntries;
    }

    public long getSignInSucceededCount() {
        return mSignInSucceededCount;
    }

    public long getSignInFailedCount() {
        return mSignInFailedCount;
    }

    public long getRecordLibraryViewedCount() {
        return mRecordLibraryViewedCount;
    }

    public long getMarkdownExportedCount() {
        return mMarkdownExportedCount;
    }

    public long getPdfExportViewedCount() {
        return mPdfExportViewedCount;
    }

    public double getAverageWritingDaysPerActiveUser() {
        return mAverageWritingDaysPerActiveUser;
    }

    public double getAverageEntryCompletionsPerActiveUser() {
        return mAverageEntryCompletionsPerActiveUser;
    }

    public double getGoalCompletionRatePercent() {
        return mGoalCompletionRatePercent;
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
}
