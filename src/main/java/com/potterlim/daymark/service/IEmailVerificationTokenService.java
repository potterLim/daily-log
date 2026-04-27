package com.potterlim.daymark.service;

import java.util.Optional;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;

public interface IEmailVerificationTokenService {

    String issueEmailVerificationToken(UserAccount userAccount);

    boolean isEmailVerificationTokenValid(String rawTokenOrNull);

    Optional<UserAccountId> verifyEmailAddress(String rawTokenOrNull);
}
