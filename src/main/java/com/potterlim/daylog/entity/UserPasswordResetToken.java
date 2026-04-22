package com.potterlim.daylog.entity;

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
@Table(name = "user_password_reset_token")
public class UserPasswordResetToken {

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

    @Column(name = "used_at")
    private LocalDateTime mUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime mCreatedAt;

    protected UserPasswordResetToken() {
    }

    private UserPasswordResetToken(UserAccount userAccount, String tokenHash, LocalDateTime expiresAt) {
        mUserAccount = userAccount;
        mTokenHash = tokenHash;
        mExpiresAt = expiresAt;
    }

    public static UserPasswordResetToken issueToken(
        UserAccount userAccount,
        String tokenHash,
        LocalDateTime expiresAt
    ) {
        return new UserPasswordResetToken(userAccount, tokenHash, expiresAt);
    }

    public UserAccountId getUserAccountId() {
        return mUserAccount.getUserAccountId();
    }

    public boolean isAvailableAt(LocalDateTime occurredAt) {
        return mUsedAt == null && mExpiresAt.isAfter(occurredAt);
    }

    public void markUsedAt(LocalDateTime usedAt) {
        mUsedAt = usedAt;
    }

    @PrePersist
    public void handleBeforePersist() {
        mCreatedAt = LocalDateTime.now();
    }
}
