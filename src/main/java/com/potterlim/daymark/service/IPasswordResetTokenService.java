package com.potterlim.daymark.service;

import java.util.Optional;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;

public interface IPasswordResetTokenService {

    /**
     * Issues a new one-time password reset token for the given user account.
     *
     * <p>Preconditions: the user account must not be null.</p>
     *
     * @return The raw token that should only be delivered through the reset channel.
     */
    String issuePasswordResetToken(UserAccount userAccount);

    /**
     * Checks whether the raw password reset token is still active.
     *
     * <p>Preconditions: the raw token may be null or blank. In that case, the method returns
     * false.</p>
     */
    boolean isPasswordResetTokenValid(String rawTokenOrNull);

    /**
     * Consumes the given password reset token exactly once and returns the owner account id.
     *
     * <p>Preconditions: the raw token may be null or blank. In that case, the method returns an
     * empty result.</p>
     */
    Optional<UserAccountId> consumePasswordResetToken(String rawTokenOrNull);
}
