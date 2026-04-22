package com.potterlim.daylog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(IAlertNotificationService.class)
public class LoggingAlertNotificationService implements IAlertNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAlertNotificationService.class);

    @Override
    public void sendOperationalAlert(String alertType, String message) {
        LOGGER.error("OPERATIONS_ALERT alertType={} message={}", alertType, message);
    }
}
