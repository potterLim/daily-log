package com.potterlim.daylog.service;

import com.potterlim.daylog.entity.UserAccount;

public interface IAuthenticationMailService {

    /**
     * Sends the email ownership verification link to the user through the configured delivery channel.
     *
     * <p>Preconditions: the user account and verification url must not be null.</p>
     */
    void sendEmailVerificationMail(UserAccount userAccount, String verificationUrl);

    /**
     * Sends the password reset link to the user through the configured delivery channel.
     *
     * <p>Preconditions: the user account and reset password url must not be null.</p>
     */
    void sendPasswordResetMail(UserAccount userAccount, String resetPasswordUrl);
}
