package com.potterlim.daylog.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "day-log")
@Validated
public class DayLogApplicationProperties {

    private final AccountProperties mAccount = new AccountProperties();
    private final MailProperties mMail = new MailProperties();
    private final OperationsProperties mOperations = new OperationsProperties();
    private final SecurityProperties mSecurity = new SecurityProperties();

    public AccountProperties getAccount() {
        return mAccount;
    }

    public MailProperties getMail() {
        return mMail;
    }

    public OperationsProperties getOperations() {
        return mOperations;
    }

    public SecurityProperties getSecurity() {
        return mSecurity;
    }

    public static final class AccountProperties {

        @Positive
        private int mPasswordResetTokenValidityMinutes = 30;
        @Positive
        private int mEmailVerificationTokenValidityMinutes = 1_440;

        public int getPasswordResetTokenValidityMinutes() {
            return mPasswordResetTokenValidityMinutes;
        }

        public void setPasswordResetTokenValidityMinutes(int passwordResetTokenValidityMinutes) {
            mPasswordResetTokenValidityMinutes = passwordResetTokenValidityMinutes;
        }

        public int getEmailVerificationTokenValidityMinutes() {
            return mEmailVerificationTokenValidityMinutes;
        }

        public void setEmailVerificationTokenValidityMinutes(int emailVerificationTokenValidityMinutes) {
            mEmailVerificationTokenValidityMinutes = emailVerificationTokenValidityMinutes;
        }
    }

    public static final class MailProperties {

        @NotBlank
        private String mFromAddress = "no-reply@daylog.local";

        public String getFromAddress() {
            return mFromAddress;
        }

        public void setFromAddress(String fromAddress) {
            mFromAddress = fromAddress;
        }
    }

    public static final class OperationsProperties {

        private String mAlertWebhookUrl = "";

        public String getAlertWebhookUrl() {
            return mAlertWebhookUrl;
        }

        public void setAlertWebhookUrl(String alertWebhookUrl) {
            mAlertWebhookUrl = alertWebhookUrl;
        }
    }

    public static final class SecurityProperties {

        @NotBlank
        private String mRememberMeKey;
        @NotBlank
        private String mRememberMeCookieName = "DAY_LOG_REMEMBER_ME";
        @Positive
        private int mRememberMeTokenValiditySeconds = 1_209_600;

        public String getRememberMeKey() {
            return mRememberMeKey;
        }

        public void setRememberMeKey(String rememberMeKey) {
            mRememberMeKey = rememberMeKey;
        }

        public String getRememberMeCookieName() {
            return mRememberMeCookieName;
        }

        public void setRememberMeCookieName(String rememberMeCookieName) {
            mRememberMeCookieName = rememberMeCookieName;
        }

        public int getRememberMeTokenValiditySeconds() {
            return mRememberMeTokenValiditySeconds;
        }

        public void setRememberMeTokenValiditySeconds(int rememberMeTokenValiditySeconds) {
            mRememberMeTokenValiditySeconds = rememberMeTokenValiditySeconds;
        }
    }
}
