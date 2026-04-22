package com.potterlim.daylog.service;

import com.potterlim.daylog.entity.UserAccount;

public interface IEmailVerificationTokenService {

    String issueEmailVerificationToken(UserAccount userAccount);

    boolean isEmailVerificationTokenValid(String rawTokenOrNull);

    boolean verifyEmailAddress(String rawTokenOrNull);
}
