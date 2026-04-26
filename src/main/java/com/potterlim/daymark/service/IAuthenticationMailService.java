package com.potterlim.daymark.service;

import com.potterlim.daymark.entity.UserAccount;

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
