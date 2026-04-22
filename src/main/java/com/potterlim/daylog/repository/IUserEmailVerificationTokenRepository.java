package com.potterlim.daylog.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.entity.UserEmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IUserEmailVerificationTokenRepository extends JpaRepository<UserEmailVerificationToken, Long> {

    @Modifying
    @Query("""
        update UserEmailVerificationToken userEmailVerificationToken
        set userEmailVerificationToken.mConsumedAt = :consumedAt
        where userEmailVerificationToken.mUserAccount = :userAccount
            and userEmailVerificationToken.mConsumedAt is null
            and userEmailVerificationToken.mExpiresAt > :occurredAt
        """)
    int expireActiveTokens(
        @Param("userAccount") UserAccount userAccount,
        @Param("occurredAt") LocalDateTime occurredAt,
        @Param("consumedAt") LocalDateTime consumedAt
    );

    @Query("""
        select userEmailVerificationToken
        from UserEmailVerificationToken userEmailVerificationToken
        where userEmailVerificationToken.mTokenHash = :tokenHash
        """)
    Optional<UserEmailVerificationToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select userEmailVerificationToken
        from UserEmailVerificationToken userEmailVerificationToken
        join fetch userEmailVerificationToken.mUserAccount userAccount
        where userEmailVerificationToken.mTokenHash = :tokenHash
        """)
    Optional<UserEmailVerificationToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
