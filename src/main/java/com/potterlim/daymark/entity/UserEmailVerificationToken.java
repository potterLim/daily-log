package com.potterlim.daymark.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_email_verification_token")
public class UserEmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long mId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount mUserAccount;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String mTokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime mExpiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime mConsumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime mCreatedAt;

    protected UserEmailVerificationToken() {
    }

    private UserEmailVerificationToken(UserAccount userAccount, String tokenHash, LocalDateTime expiresAt) {
        mUserAccount = userAccount;
        mTokenHash = tokenHash;
        mExpiresAt = expiresAt;
    }

    public static UserEmailVerificationToken issueToken(
        UserAccount userAccount,
        String tokenHash,
        LocalDateTime expiresAt
    ) {
        return new UserEmailVerificationToken(userAccount, tokenHash, expiresAt);
    }

    public UserAccount getUserAccount() {
        return mUserAccount;
    }

    public boolean isAvailableAt(LocalDateTime occurredAt) {
        return mConsumedAt == null && mExpiresAt.isAfter(occurredAt);
    }

    public void markConsumedAt(LocalDateTime consumedAt) {
        mConsumedAt = consumedAt;
    }

    @PrePersist
    public void handleBeforePersist() {
        mCreatedAt = LocalDateTime.now();
    }
}
