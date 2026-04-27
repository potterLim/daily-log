package com.potterlim.daymark.config;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
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
    private static final String DEFAULT_MAIL_FROM_ADDRESS = "no-reply@daymark.local";

    private final DaymarkApplicationProperties mDaymarkApplicationProperties;
    private final Environment mEnvironment;
    private final ObjectProvider<JavaMailSender> mJavaMailSenderProvider;

    public ProductionReadinessValidator(
        DaymarkApplicationProperties daymarkApplicationProperties,
        Environment environment,
        ObjectProvider<JavaMailSender> javaMailSenderProvider
    ) {
        mDaymarkApplicationProperties = daymarkApplicationProperties;
        mEnvironment = environment;
        mJavaMailSenderProvider = javaMailSenderProvider;
    }

    @PostConstruct
    public void validateProductionReadiness() {
        List<String> validationErrors = new ArrayList<>();

        validateRememberMeKey(validationErrors);
        validatePublicBaseUrl(validationErrors);
        validateSecureSessionCookie(validationErrors);
        validateSmtpConfiguration(validationErrors);
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

    private void validateSmtpConfiguration(List<String> validationErrors) {
        if (!mDaymarkApplicationProperties.getOperations().getProductionReadiness().isRequireSmtp()) {
            return;
        }

        if (mJavaMailSenderProvider.getIfAvailable() == null) {
            validationErrors.add("SMTP must be configured in production so verification and recovery mail can be sent.");
        }

        requireTextProperty(
            validationErrors,
            "spring.mail.host",
            "spring.mail.host must be configured for AWS SES SMTP."
        );
        requireTextProperty(
            validationErrors,
            "spring.mail.username",
            "spring.mail.username must contain the AWS SES SMTP user."
        );
        requireTextProperty(
            validationErrors,
            "spring.mail.password",
            "spring.mail.password must contain the AWS SES SMTP password."
        );

        Integer smtpPort = mEnvironment.getProperty("spring.mail.port", Integer.class);
        if (smtpPort == null || smtpPort <= 0) {
            validationErrors.add("spring.mail.port must be configured for AWS SES SMTP.");
        }

        if (!isTrueProperty("spring.mail.properties.mail.smtp.auth")) {
            validationErrors.add("spring.mail.properties.mail.smtp.auth must be true for AWS SES SMTP.");
        }

        if (!isTrueProperty("spring.mail.properties.mail.smtp.starttls.enable")) {
            validationErrors.add("spring.mail.properties.mail.smtp.starttls.enable must be true for AWS SES SMTP.");
        }

        String mailFromAddress = mDaymarkApplicationProperties.getMail().getFromAddress();
        if (mailFromAddress == null || mailFromAddress.isBlank()) {
            validationErrors.add("daymark.mail.from-address must not be blank in production.");
            return;
        }

        if (DEFAULT_MAIL_FROM_ADDRESS.equalsIgnoreCase(mailFromAddress)) {
            validationErrors.add("daymark.mail.from-address must be replaced with a real sender address in production.");
        }

        String normalizedMailFromAddress = mailFromAddress.strip().toLowerCase();
        if (
            normalizedMailFromAddress.endsWith("@example.com")
                || normalizedMailFromAddress.endsWith(".local")
                || normalizedMailFromAddress.contains("@localhost")
        ) {
            validationErrors.add("daymark.mail.from-address must use the verified production mail domain.");
        }
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

    private boolean isTrueProperty(String propertyName) {
        return mEnvironment.getProperty(propertyName, Boolean.class, false);
    }

    private void requireTextProperty(List<String> validationErrors, String propertyName, String validationMessage) {
        String propertyValue = mEnvironment.getProperty(propertyName);
        if (propertyValue == null || propertyValue.isBlank()) {
            validationErrors.add(validationMessage);
        }
    }
}
