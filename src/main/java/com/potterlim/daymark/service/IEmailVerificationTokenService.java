package com.potterlim.daymark.service;

import com.potterlim.daymark.entity.UserAccount;

public interface IEmailVerificationTokenService {

    String issueEmailVerificationToken(UserAccount userAccount);

    boolean isEmailVerificationTokenValid(String rawTokenOrNull);

    boolean verifyEmailAddress(String rawTokenOrNull);
}
