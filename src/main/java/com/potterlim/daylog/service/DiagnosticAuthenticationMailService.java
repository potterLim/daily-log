package com.potterlim.daylog.service;

import com.potterlim.daylog.entity.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(JavaMailSender.class)
public class DiagnosticAuthenticationMailService implements IAuthenticationMailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticAuthenticationMailService.class);

    private final Environment mEnvironment;

    public DiagnosticAuthenticationMailService(Environment environment) {
        mEnvironment = environment;
    }

    @Override
    public void sendEmailVerificationMail(UserAccount userAccount, String verificationUrl) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        if (verificationUrl == null || verificationUrl.isBlank()) {
            throw new IllegalArgumentException("verificationUrl must not be blank.");
        }

        if (mEnvironment.acceptsProfiles(Profiles.of("local", "test", "mysql-integration-test"))) {
            LOGGER.info(
                "Email verification mail delivery is running in diagnostic mode. userName={}, emailAddress={}, verificationUrl={}",
                userAccount.getUsername(),
                userAccount.getEmailAddress(),
                verificationUrl
            );
            return;
        }

        LOGGER.warn(
            "Email verification mail requested for emailAddress={} but SMTP is not configured.",
            userAccount.getEmailAddress()
        );
    }

    @Override
    public void sendPasswordResetMail(UserAccount userAccount, String resetPasswordUrl) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        if (resetPasswordUrl == null || resetPasswordUrl.isBlank()) {
            throw new IllegalArgumentException("resetPasswordUrl must not be blank.");
        }

        if (mEnvironment.acceptsProfiles(Profiles.of("local", "test", "mysql-integration-test"))) {
            LOGGER.info(
                "Password reset mail delivery is running in diagnostic mode. userName={}, emailAddress={}, resetPasswordUrl={}",
                userAccount.getUsername(),
                userAccount.getEmailAddress(),
                resetPasswordUrl
            );
            return;
        }

        LOGGER.warn(
            "Password reset mail requested for emailAddress={} but SMTP is not configured.",
            userAccount.getEmailAddress()
        );
    }
}
