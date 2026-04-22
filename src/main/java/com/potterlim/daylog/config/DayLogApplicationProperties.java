package com.potterlim.daylog.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "day-log")
@Validated
public class DayLogApplicationProperties {

    private final SecurityProperties mSecurity = new SecurityProperties();

    public SecurityProperties getSecurity() {
        return mSecurity;
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
