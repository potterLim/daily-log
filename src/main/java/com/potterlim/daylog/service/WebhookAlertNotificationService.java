package com.potterlim.daylog.service;

import java.util.Map;
import com.potterlim.daylog.config.DayLogApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${day-log.operations.alert-webhook-url:}')")
public class WebhookAlertNotificationService implements IAlertNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookAlertNotificationService.class);

    private final RestClient mRestClient;
    private final DayLogApplicationProperties mDayLogApplicationProperties;

    public WebhookAlertNotificationService(DayLogApplicationProperties dayLogApplicationProperties) {
        mRestClient = RestClient.create();
        mDayLogApplicationProperties = dayLogApplicationProperties;
    }

    @Override
    public void sendOperationalAlert(String alertType, String message) {
        try {
            mRestClient.post()
                .uri(mDayLogApplicationProperties.getOperations().getAlertWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "application", "dayLog",
                    "alertType", alertType,
                    "message", message
                ))
                .retrieve()
                .toBodilessEntity();
        } catch (RuntimeException runtimeException) {
            LOGGER.warn(
                "Operational alert delivery failed. alertType={}, message={}",
                alertType,
                message,
                runtimeException
            );
        }
    }
}
