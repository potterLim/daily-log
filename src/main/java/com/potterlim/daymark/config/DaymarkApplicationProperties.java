package com.potterlim.daymark.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "daymark")
@Validated
public class DaymarkApplicationProperties {

    private final OperationsProperties mOperations = new OperationsProperties();
    private final SecurityProperties mSecurity = new SecurityProperties();
    private final SupportProperties mSupport = new SupportProperties();
    private String mPublicBaseUrl = "";

    public OperationsProperties getOperations() {
        return mOperations;
    }

    public SecurityProperties getSecurity() {
        return mSecurity;
    }

    public SupportProperties getSupport() {
        return mSupport;
    }

    public String getPublicBaseUrl() {
        return mPublicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        mPublicBaseUrl = publicBaseUrl;
    }

    public static final class OperationsProperties {

        private String mAlertWebhookUrl = "";
        private final ProductionReadinessProperties mProductionReadiness = new ProductionReadinessProperties();
        private final WeeklySummaryProperties mWeeklySummary = new WeeklySummaryProperties();

        public String getAlertWebhookUrl() {
            return mAlertWebhookUrl;
        }

        public void setAlertWebhookUrl(String alertWebhookUrl) {
            mAlertWebhookUrl = alertWebhookUrl;
        }

        public ProductionReadinessProperties getProductionReadiness() {
            return mProductionReadiness;
        }

        public WeeklySummaryProperties getWeeklySummary() {
            return mWeeklySummary;
        }
    }

    public static final class ProductionReadinessProperties {

        private boolean mIsEnabled;
        private boolean mShouldRequireAlertWebhook;
        private boolean mShouldRequireSecureSessionCookie;
        @Positive
        private int mMinimumRememberMeKeyLength = 32;

        public boolean isEnabled() {
            return mIsEnabled;
        }

        public void setEnabled(boolean enabled) {
            mIsEnabled = enabled;
        }

        public boolean isRequireAlertWebhook() {
            return mShouldRequireAlertWebhook;
        }

        public void setRequireAlertWebhook(boolean requireAlertWebhook) {
            mShouldRequireAlertWebhook = requireAlertWebhook;
        }

        public boolean isRequireSecureSessionCookie() {
            return mShouldRequireSecureSessionCookie;
        }

        public void setRequireSecureSessionCookie(boolean requireSecureSessionCookie) {
            mShouldRequireSecureSessionCookie = requireSecureSessionCookie;
        }

        public int getMinimumRememberMeKeyLength() {
            return mMinimumRememberMeKeyLength;
        }

        public void setMinimumRememberMeKeyLength(int minimumRememberMeKeyLength) {
            mMinimumRememberMeKeyLength = minimumRememberMeKeyLength;
        }
    }

    public static final class WeeklySummaryProperties {

        private boolean mIsEnabled;
        @NotBlank
        private String mCron = "0 0 9 * * MON";
        @NotBlank
        private String mZone = "Asia/Seoul";

        public boolean isEnabled() {
            return mIsEnabled;
        }

        public void setEnabled(boolean enabled) {
            mIsEnabled = enabled;
        }

        public String getCron() {
            return mCron;
        }

        public void setCron(String cron) {
            mCron = cron;
        }

        public String getZone() {
            return mZone;
        }

        public void setZone(String zone) {
            mZone = zone;
        }
    }

    public static final class SecurityProperties {

        @NotBlank
        private String mRememberMeKey;
        @NotBlank
        private String mRememberMeCookieName = "DAYMARK_REMEMBER_ME";
        private boolean mIsRememberMeCookieSecure;
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

        public boolean isRememberMeCookieSecure() {
            return mIsRememberMeCookieSecure;
        }

        public void setRememberMeCookieSecure(boolean rememberMeCookieSecure) {
            mIsRememberMeCookieSecure = rememberMeCookieSecure;
        }

        public int getRememberMeTokenValiditySeconds() {
            return mRememberMeTokenValiditySeconds;
        }

        public void setRememberMeTokenValiditySeconds(int rememberMeTokenValiditySeconds) {
            mRememberMeTokenValiditySeconds = rememberMeTokenValiditySeconds;
        }
    }

    public static final class SupportProperties {

        @NotBlank
        @Email
        private String mContactEmail = "potterLim0808@gmail.com";

        public String getContactEmail() {
            return mContactEmail;
        }

        public void setContactEmail(String contactEmail) {
            mContactEmail = contactEmail;
        }
    }
}
