package com.potterlim.daymark.service;

import com.potterlim.daymark.config.DaymarkApplicationProperties;
import com.potterlim.daymark.entity.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AuthenticationMailWorkflowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationMailWorkflowService.class);

    private final IPasswordResetTokenService mPasswordResetTokenService;
    private final IEmailVerificationTokenService mEmailVerificationTokenService;
    private final IAuthenticationMailService mAuthenticationMailService;
    private final IAlertNotificationService mAlertNotificationService;
    private final DaymarkApplicationProperties mDaymarkApplicationProperties;

    public AuthenticationMailWorkflowService(
        IPasswordResetTokenService passwordResetTokenService,
        IEmailVerificationTokenService emailVerificationTokenService,
        IAuthenticationMailService authenticationMailService,
        IAlertNotificationService alertNotificationService,
        DaymarkApplicationProperties daymarkApplicationProperties
    ) {
        mPasswordResetTokenService = passwordResetTokenService;
        mEmailVerificationTokenService = emailVerificationTokenService;
        mAuthenticationMailService = authenticationMailService;
        mAlertNotificationService = alertNotificationService;
        mDaymarkApplicationProperties = daymarkApplicationProperties;
    }

    public boolean sendRecoveryInstructions(UserAccount userAccount, HttpServletRequest httpServletRequest) {
        if (userAccount.hasVerifiedEmailAddress()) {
            return sendPasswordResetInstructions(userAccount, httpServletRequest);
        }

        return sendEmailVerificationInstructions(userAccount, httpServletRequest);
    }

    public boolean sendPasswordResetInstructions(UserAccount userAccount, HttpServletRequest httpServletRequest) {
        try {
            String rawPasswordResetToken = mPasswordResetTokenService.issuePasswordResetToken(userAccount);
            String resetPasswordUrl = buildAbsoluteUrl(
                httpServletRequest,
                "/reset-password",
                "token",
                rawPasswordResetToken
            );

            mAuthenticationMailService.sendPasswordResetMail(userAccount, resetPasswordUrl);
            return true;
        } catch (RuntimeException runtimeException) {
            reportMailDeliveryFailure("password-reset-mail-failed", userAccount, runtimeException);
            return false;
        }
    }

    public boolean sendEmailVerificationInstructions(UserAccount userAccount, HttpServletRequest httpServletRequest) {
        try {
            if (userAccount.hasVerifiedEmailAddress()) {
                return true;
            }

            String rawEmailVerificationToken = mEmailVerificationTokenService.issueEmailVerificationToken(userAccount);
            String verificationUrl = buildAbsoluteUrl(
                httpServletRequest,
                "/verify-email",
                "token",
                rawEmailVerificationToken
            );

            mAuthenticationMailService.sendEmailVerificationMail(userAccount, verificationUrl);
            return true;
        } catch (RuntimeException runtimeException) {
            reportMailDeliveryFailure("email-verification-mail-failed", userAccount, runtimeException);
            return false;
        }
    }

    private String buildAbsoluteUrl(
        HttpServletRequest httpServletRequest,
        String path,
        String queryParameterName,
        String queryParameterValue
    ) {
        String publicBaseUrl = mDaymarkApplicationProperties.getPublicBaseUrl();
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return UriComponentsBuilder.fromUriString(publicBaseUrl.strip())
                .replacePath(path)
                .replaceQuery(null)
                .queryParam(queryParameterName, queryParameterValue)
                .build()
                .toUriString();
        }

        return ServletUriComponentsBuilder.fromRequestUri(httpServletRequest)
            .replacePath(httpServletRequest.getContextPath() + path)
            .replaceQuery(null)
            .queryParam(queryParameterName, queryParameterValue)
            .build()
            .toUriString();
    }

    private void reportMailDeliveryFailure(
        String alertType,
        UserAccount userAccount,
        RuntimeException runtimeException
    ) {
        LOGGER.error(
            "Authentication mail delivery failed. alertType={}, userName={}, emailAddress={}",
            alertType,
            userAccount.getUsername(),
            userAccount.getEmailAddress(),
            runtimeException
        );
        mAlertNotificationService.sendOperationalAlert(
            alertType,
            "userName=%s, emailAddress=%s".formatted(userAccount.getUsername(), userAccount.getEmailAddress())
        );
    }
}
