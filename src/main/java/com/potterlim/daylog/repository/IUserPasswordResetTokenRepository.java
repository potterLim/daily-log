package com.potterlim.daylog.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.entity.UserPasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IUserPasswordResetTokenRepository extends JpaRepository<UserPasswordResetToken, Long> {

    @Modifying
    @Query("""
        update UserPasswordResetToken userPasswordResetToken
        set userPasswordResetToken.mUsedAt = :usedAt
        where userPasswordResetToken.mUserAccount = :userAccount
            and userPasswordResetToken.mUsedAt is null
            and userPasswordResetToken.mExpiresAt > :occurredAt
        """)
    int expireActiveTokens(
        @Param("userAccount") UserAccount userAccount,
        @Param("occurredAt") LocalDateTime occurredAt,
        @Param("usedAt") LocalDateTime usedAt
    );

    @Query("""
        select userPasswordResetToken
        from UserPasswordResetToken userPasswordResetToken
        where userPasswordResetToken.mTokenHash = :tokenHash
        """)
    Optional<UserPasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select userPasswordResetToken
        from UserPasswordResetToken userPasswordResetToken
        join fetch userPasswordResetToken.mUserAccount userAccount
        where userPasswordResetToken.mTokenHash = :tokenHash
        """)
    Optional<UserPasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
