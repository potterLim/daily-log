package com.potterlim.daymark.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "operation_usage_event")
public class OperationUsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long mId;

    @Column(name = "user_account_id")
    private Long mUserAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private EOperationEventType mEventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate mEventDate;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime mOccurredAt;

    protected OperationUsageEvent() {
    }

    private OperationUsageEvent(
        EOperationEventType eventType,
        UserAccountId userAccountIdOrNull,
        LocalDateTime occurredAt
    ) {
        mEventType = eventType;
        mUserAccountId = userAccountIdOrNull == null ? null : userAccountIdOrNull.getValue();
        mOccurredAt = occurredAt;
        mEventDate = occurredAt.toLocalDate();
    }

    public static OperationUsageEvent record(
        EOperationEventType eventType,
        UserAccountId userAccountIdOrNull,
        LocalDateTime occurredAt
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType must not be null.");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null.");
        }

        return new OperationUsageEvent(eventType, userAccountIdOrNull, occurredAt);
    }

    public Long getUserAccountIdOrNull() {
        return mUserAccountId;
    }

    public EOperationEventType getEventType() {
        return mEventType;
    }

    public LocalDate getEventDate() {
        return mEventDate;
    }

    public LocalDateTime getOccurredAt() {
        return mOccurredAt;
    }
}
