package com.potterlim.daymark.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import com.potterlim.daymark.config.DaymarkApplicationProperties;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.entity.UserEmailVerificationToken;
import com.potterlim.daymark.repository.IUserEmailVerificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationTokenService implements IEmailVerificationTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final IUserEmailVerificationTokenRepository mUserEmailVerificationTokenRepository;
    private final DaymarkApplicationProperties mDaymarkApplicationProperties;
    private final SecureRandom mSecureRandom = new SecureRandom();

    public EmailVerificationTokenService(
        IUserEmailVerificationTokenRepository userEmailVerificationTokenRepository,
        DaymarkApplicationProperties daymarkApplicationProperties
    ) {
        mUserEmailVerificationTokenRepository = userEmailVerificationTokenRepository;
        mDaymarkApplicationProperties = daymarkApplicationProperties;
    }

    @Override
    @Transactional
    public String issueEmailVerificationToken(UserAccount userAccount) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        if (userAccount.hasVerifiedEmailAddress()) {
            throw new IllegalStateException("A verified email address must not receive a new verification token.");
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt =
            issuedAt.plusMinutes(mDaymarkApplicationProperties.getAccount().getEmailVerificationTokenValidityMinutes());
        String rawToken = AuthenticationTokenSupport.generateRawToken(mSecureRandom, TOKEN_BYTE_LENGTH);

        mUserEmailVerificationTokenRepository.expireActiveTokens(userAccount, issuedAt, issuedAt);
        mUserEmailVerificationTokenRepository.save(
            UserEmailVerificationToken.issueToken(
                userAccount,
                AuthenticationTokenSupport.hashToken(rawToken),
                expiresAt
            )
        );

        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailVerificationTokenValid(String rawTokenOrNull) {
        if (rawTokenOrNull == null || rawTokenOrNull.isBlank()) {
            return false;
        }

        return mUserEmailVerificationTokenRepository.findByTokenHash(
                AuthenticationTokenSupport.hashToken(rawTokenOrNull)
            )
            .filter(userEmailVerificationToken -> userEmailVerificationToken.isAvailableAt(LocalDateTime.now()))
            .isPresent();
    }

    @Override
    @Transactional
    public Optional<UserAccountId> verifyEmailAddress(String rawTokenOrNull) {
        if (rawTokenOrNull == null || rawTokenOrNull.isBlank()) {
            return Optional.empty();
        }

        LocalDateTime consumedAt = LocalDateTime.now();

        Optional<UserEmailVerificationToken> userEmailVerificationTokenOrEmpty =
            mUserEmailVerificationTokenRepository.findByTokenHashForUpdate(
                AuthenticationTokenSupport.hashToken(rawTokenOrNull)
            )
                .filter(userEmailVerificationToken -> userEmailVerificationToken.isAvailableAt(consumedAt));

        if (userEmailVerificationTokenOrEmpty.isEmpty()) {
            return Optional.empty();
        }

        UserEmailVerificationToken userEmailVerificationToken = userEmailVerificationTokenOrEmpty.get();
        UserAccount userAccount = userEmailVerificationToken.getUserAccount();
        userEmailVerificationToken.markConsumedAt(consumedAt);
        userAccount.markEmailAddressVerified(consumedAt);
        return Optional.of(userAccount.getUserAccountId());
    }
}
