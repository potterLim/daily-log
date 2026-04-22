package com.potterlim.daylog.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import com.potterlim.daylog.config.DayLogApplicationProperties;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.entity.UserEmailVerificationToken;
import com.potterlim.daylog.repository.IUserEmailVerificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationTokenService implements IEmailVerificationTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final IUserEmailVerificationTokenRepository mUserEmailVerificationTokenRepository;
    private final DayLogApplicationProperties mDayLogApplicationProperties;
    private final SecureRandom mSecureRandom = new SecureRandom();

    public EmailVerificationTokenService(
        IUserEmailVerificationTokenRepository userEmailVerificationTokenRepository,
        DayLogApplicationProperties dayLogApplicationProperties
    ) {
        mUserEmailVerificationTokenRepository = userEmailVerificationTokenRepository;
        mDayLogApplicationProperties = dayLogApplicationProperties;
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
            issuedAt.plusMinutes(mDayLogApplicationProperties.getAccount().getEmailVerificationTokenValidityMinutes());
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

        return mUserEmailVerificationTokenRepository.findByTokenHash(AuthenticationTokenSupport.hashToken(rawTokenOrNull))
            .filter(userEmailVerificationToken -> userEmailVerificationToken.isAvailableAt(LocalDateTime.now()))
            .isPresent();
    }

    @Override
    @Transactional
    public boolean verifyEmailAddress(String rawTokenOrNull) {
        if (rawTokenOrNull == null || rawTokenOrNull.isBlank()) {
            return false;
        }

        LocalDateTime consumedAt = LocalDateTime.now();

        return mUserEmailVerificationTokenRepository.findByTokenHashForUpdate(AuthenticationTokenSupport.hashToken(rawTokenOrNull))
            .filter(userEmailVerificationToken -> userEmailVerificationToken.isAvailableAt(consumedAt))
            .map(userEmailVerificationToken -> {
                UserAccount userAccount = userEmailVerificationToken.getUserAccount();
                userEmailVerificationToken.markConsumedAt(consumedAt);
                userAccount.markEmailAddressVerified(consumedAt);
                return true;
            })
            .orElse(false);
    }
}
