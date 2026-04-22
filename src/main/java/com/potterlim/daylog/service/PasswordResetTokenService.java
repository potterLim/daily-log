package com.potterlim.daylog.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import com.potterlim.daylog.config.DayLogApplicationProperties;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.entity.UserAccountId;
import com.potterlim.daylog.entity.UserPasswordResetToken;
import com.potterlim.daylog.repository.IUserPasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetTokenService implements IPasswordResetTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final IUserPasswordResetTokenRepository mUserPasswordResetTokenRepository;
    private final DayLogApplicationProperties mDayLogApplicationProperties;
    private final SecureRandom mSecureRandom = new SecureRandom();

    public PasswordResetTokenService(
        IUserPasswordResetTokenRepository userPasswordResetTokenRepository,
        DayLogApplicationProperties dayLogApplicationProperties
    ) {
        mUserPasswordResetTokenRepository = userPasswordResetTokenRepository;
        mDayLogApplicationProperties = dayLogApplicationProperties;
    }

    @Override
    @Transactional
    public String issuePasswordResetToken(UserAccount userAccount) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt =
            issuedAt.plusMinutes(mDayLogApplicationProperties.getAccount().getPasswordResetTokenValidityMinutes());
        String rawToken = AuthenticationTokenSupport.generateRawToken(mSecureRandom, TOKEN_BYTE_LENGTH);

        mUserPasswordResetTokenRepository.expireActiveTokens(userAccount, issuedAt, issuedAt);
        mUserPasswordResetTokenRepository.save(
            UserPasswordResetToken.issueToken(
                userAccount,
                AuthenticationTokenSupport.hashToken(rawToken),
                expiresAt
            )
        );

        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPasswordResetTokenValid(String rawTokenOrNull) {
        if (rawTokenOrNull == null || rawTokenOrNull.isBlank()) {
            return false;
        }

        return mUserPasswordResetTokenRepository.findByTokenHash(AuthenticationTokenSupport.hashToken(rawTokenOrNull))
            .filter(userPasswordResetToken -> userPasswordResetToken.isAvailableAt(LocalDateTime.now()))
            .isPresent();
    }

    @Override
    @Transactional
    public Optional<UserAccountId> consumePasswordResetToken(String rawTokenOrNull) {
        if (rawTokenOrNull == null || rawTokenOrNull.isBlank()) {
            return Optional.empty();
        }

        LocalDateTime consumedAt = LocalDateTime.now();

        return mUserPasswordResetTokenRepository.findByTokenHashForUpdate(AuthenticationTokenSupport.hashToken(rawTokenOrNull))
            .filter(userPasswordResetToken -> userPasswordResetToken.isAvailableAt(consumedAt))
            .map(userPasswordResetToken -> {
                userPasswordResetToken.markUsedAt(consumedAt);
                return userPasswordResetToken.getUserAccountId();
            });
    }
}
