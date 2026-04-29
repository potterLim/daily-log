package com.potterlim.daymark.config;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
@ConditionalOnProperty(
    prefix = "daymark.operations.production-readiness",
    name = "enabled",
    havingValue = "true"
)
public class ProductionReadinessValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionReadinessValidator.class);

    private final DaymarkApplicationProperties mDaymarkApplicationProperties;
    private final Environment mEnvironment;

    public ProductionReadinessValidator(
        DaymarkApplicationProperties daymarkApplicationProperties,
        Environment environment
    ) {
        mDaymarkApplicationProperties = daymarkApplicationProperties;
        mEnvironment = environment;
    }

    @PostConstruct
    public void validateProductionReadiness() {
        List<String> validationErrors = new ArrayList<>();

        validateRememberMeKey(validationErrors);
        validatePublicBaseUrl(validationErrors);
        validateSecureSessionCookie(validationErrors);
        validateGoogleOAuthConfiguration(validationErrors);
        validateAlertWebhook(validationErrors);

        if (!validationErrors.isEmpty()) {
            throw new IllegalStateException(
                "Production readiness validation failed:\n- " + String.join("\n- ", validationErrors)
            );
        }

        LOGGER.info("Production readiness validation passed.");
    }

    private void validatePublicBaseUrl(List<String> validationErrors) {
        String publicBaseUrl = mDaymarkApplicationProperties.getPublicBaseUrl();
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            validationErrors.add("daymark.public-base-url must be configured in production.");
            return;
        }

        String normalizedPublicBaseUrl = publicBaseUrl.strip().toLowerCase();
        if (!normalizedPublicBaseUrl.startsWith("https://")) {
            validationErrors.add("daymark.public-base-url must start with https:// in production.");
        }

        if (
            normalizedPublicBaseUrl.contains("localhost")
                || normalizedPublicBaseUrl.contains("127.0.0.1")
                || normalizedPublicBaseUrl.contains("example.com")
        ) {
            validationErrors.add("daymark.public-base-url must use the real production domain.");
        }
    }

    private void validateRememberMeKey(List<String> validationErrors) {
        String rememberMeKey = mDaymarkApplicationProperties.getSecurity().getRememberMeKey();
        int minimumRememberMeKeyLength =
            mDaymarkApplicationProperties.getOperations().getProductionReadiness().getMinimumRememberMeKeyLength();

        if (rememberMeKey == null || rememberMeKey.isBlank()) {
            validationErrors.add("daymark.security.remember-me-key must not be blank.");
            return;
        }

        if (rememberMeKey.length() < minimumRememberMeKeyLength) {
            validationErrors.add(
                "daymark.security.remember-me-key must be at least %d characters long."
                    .formatted(minimumRememberMeKeyLength)
            );
        }

        String normalizedRememberMeKey = rememberMeKey.trim().toLowerCase();
        if (
            normalizedRememberMeKey.contains("change-this")
                || normalizedRememberMeKey.contains("remember-me-key")
                || normalizedRememberMeKey.contains("local-development")
                || normalizedRememberMeKey.contains("test-remember")
        ) {
            validationErrors.add("daymark.security.remember-me-key must not use a placeholder or development secret.");
        }
    }

    private void validateSecureSessionCookie(List<String> validationErrors) {
        if (!mDaymarkApplicationProperties.getOperations().getProductionReadiness().isRequireSecureSessionCookie()) {
            return;
        }

        boolean isSecureSessionCookieEnabled =
            mEnvironment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false);
        if (!isSecureSessionCookieEnabled) {
            validationErrors.add("server.servlet.session.cookie.secure must be true in production.");
        }

        if (!mDaymarkApplicationProperties.getSecurity().isRememberMeCookieSecure()) {
            validationErrors.add("daymark.security.remember-me-cookie-secure must be true in production.");
        }
    }

    private void validateGoogleOAuthConfiguration(List<String> validationErrors) {
        requireTextProperty(
            validationErrors,
            "spring.security.oauth2.client.registration.google.client-id",
            "Google OAuth client id must be configured in production."
        );
        requireTextProperty(
            validationErrors,
            "spring.security.oauth2.client.registration.google.client-secret",
            "Google OAuth client secret must be configured in production."
        );
    }

    private void validateAlertWebhook(List<String> validationErrors) {
        if (!mDaymarkApplicationProperties.getOperations().getProductionReadiness().isRequireAlertWebhook()) {
            return;
        }

        String alertWebhookUrl = mDaymarkApplicationProperties.getOperations().getAlertWebhookUrl();
        if (alertWebhookUrl == null || alertWebhookUrl.isBlank()) {
            validationErrors.add("daymark.operations.alert-webhook-url must be configured in production.");
            return;
        }

        String normalizedAlertWebhookUrl = alertWebhookUrl.strip().toLowerCase();
        if (!normalizedAlertWebhookUrl.startsWith("https://")) {
            validationErrors.add("daymark.operations.alert-webhook-url must start with https:// in production.");
        }

        if (normalizedAlertWebhookUrl.contains("example.com")) {
            validationErrors.add("daymark.operations.alert-webhook-url must not use a placeholder URL.");
        }
    }

    private void requireTextProperty(List<String> validationErrors, String propertyName, String validationMessage) {
        String propertyValue = mEnvironment.getProperty(propertyName);
        if (propertyValue == null || propertyValue.isBlank()) {
            validationErrors.add(validationMessage);
        }
    }
}
