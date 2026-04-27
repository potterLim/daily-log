package com.potterlim.daymark.service;

import java.time.Clock;
import java.time.LocalDateTime;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.OperationUsageEvent;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.repository.IOperationUsageEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OperationUsageEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationUsageEventService.class);

    private final IOperationUsageEventRepository mOperationUsageEventRepository;
    private final Clock mClock;

    public OperationUsageEventService(IOperationUsageEventRepository operationUsageEventRepository, Clock clock) {
        mOperationUsageEventRepository = operationUsageEventRepository;
        mClock = clock;
    }

    public void recordUserEvent(EOperationEventType eventType, UserAccountId userAccountId) {
        recordEvent(eventType, userAccountId);
    }

    public void recordAnonymousEvent(EOperationEventType eventType) {
        recordEvent(eventType, null);
    }

    private void recordEvent(EOperationEventType eventType, UserAccountId userAccountIdOrNull) {
        try {
            OperationUsageEvent operationUsageEvent = OperationUsageEvent.record(
                eventType,
                userAccountIdOrNull,
                LocalDateTime.now(mClock)
            );
            mOperationUsageEventRepository.save(operationUsageEvent);
        } catch (RuntimeException runtimeException) {
            LOGGER.warn("Operation usage event recording failed. eventType={}", eventType, runtimeException);
        }
    }
}
