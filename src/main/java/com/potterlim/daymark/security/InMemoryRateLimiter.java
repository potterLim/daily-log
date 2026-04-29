package com.potterlim.daymark.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter {

    private static final int CLEANUP_INTERVAL_REQUEST_COUNT = 1_000;

    private final Clock mClock;
    private final ConcurrentMap<String, RateLimitWindow> mWindowsByKey = new ConcurrentHashMap<>();
    private final AtomicInteger mRequestCountSinceCleanup = new AtomicInteger();

    public InMemoryRateLimiter(Clock clock) {
        mClock = clock;
    }

    public boolean tryAcquire(String key, int requestLimit, Duration windowDuration) {
        long currentTimeMillis = mClock.millis();
        long windowDurationMillis = windowDuration.toMillis();
        RateLimitWindow rateLimitWindow = mWindowsByKey.compute(key, (ignoredKey, currentWindowOrNull) -> {
            if (currentWindowOrNull == null || currentWindowOrNull.hasExpired(currentTimeMillis)) {
                return new RateLimitWindow(currentTimeMillis + windowDurationMillis, 1);
            }

            currentWindowOrNull.incrementRequestCount();
            return currentWindowOrNull;
        });

        cleanupExpiredWindowsIfNeeded(currentTimeMillis);
        return rateLimitWindow.getRequestCount() <= requestLimit;
    }

    public void clear() {
        mWindowsByKey.clear();
        mRequestCountSinceCleanup.set(0);
    }

    private void cleanupExpiredWindowsIfNeeded(long currentTimeMillis) {
        int requestCountSinceCleanup = mRequestCountSinceCleanup.incrementAndGet();
        if (requestCountSinceCleanup < CLEANUP_INTERVAL_REQUEST_COUNT) {
            return;
        }

        if (!mRequestCountSinceCleanup.compareAndSet(requestCountSinceCleanup, 0)) {
            return;
        }

        for (Map.Entry<String, RateLimitWindow> rateLimitWindowEntry : mWindowsByKey.entrySet()) {
            if (rateLimitWindowEntry.getValue().hasExpired(currentTimeMillis)) {
                mWindowsByKey.remove(rateLimitWindowEntry.getKey(), rateLimitWindowEntry.getValue());
            }
        }
    }

    private static final class RateLimitWindow {

        private final long mResetAtMillis;
        private int mRequestCount;

        private RateLimitWindow(long resetAtMillis, int requestCount) {
            mResetAtMillis = resetAtMillis;
            mRequestCount = requestCount;
        }

        private boolean hasExpired(long currentTimeMillis) {
            return currentTimeMillis >= mResetAtMillis;
        }

        private void incrementRequestCount() {
            mRequestCount++;
        }

        private int getRequestCount() {
            return mRequestCount;
        }
    }
}
