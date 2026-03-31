package com.potterlim.daylog.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "day-log")
@Validated
public class DayLogApplicationProperties {

    @Valid
    private final StorageProperties mStorage = new StorageProperties();
    @Valid
    private final SecurityProperties mSecurity = new SecurityProperties();

    public StorageProperties getStorage() {
        return mStorage;
    }

    public SecurityProperties getSecurity() {
        return mSecurity;
    }

    public static class StorageProperties {

        @NotBlank
        private String mLogsRootPath = "logs";

        public String getLogsRootPath() {
            return mLogsRootPath;
        }

        public void setLogsRootPath(String logsRootPath) {
            mLogsRootPath = logsRootPath;
        }
    }

    public static class SecurityProperties {

        @NotBlank
        private String mRememberMeKey;
        @NotBlank
        private String mRememberMeCookieName = "DAY_LOG_REMEMBER_ME";
        @Positive
        private int mRememberMeTokenValiditySeconds = 1209600;

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
