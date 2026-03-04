package com.savadanko.aviasales.mongo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoFailoverGuard {

    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(30);

    private final Clock clock;
    private final AtomicLong blockedUntilEpochMillis = new AtomicLong(0L);

    @Value("${app.mongo.enabled:true}")
    private boolean enabled;

    @Value("${app.mongo.retry-after:30s}")
    private Duration retryAfter;

    public boolean canUseMongo() {
        if (!enabled) {
            return false;
        }
        long now = Instant.now(clock).toEpochMilli();
        return now >= blockedUntilEpochMillis.get();
    }

    public void recordFailure(String operation, Exception exception) {
        if (!enabled) {
            return;
        }

        long now = Instant.now(clock).toEpochMilli();
        long cooldownMillis = normalizeRetryAfterMillis();
        long blockedUntil = now + cooldownMillis;

        long previous = blockedUntilEpochMillis.getAndUpdate(current -> Math.max(current, blockedUntil));

        if (now >= previous) {
            log.warn(
                    "Mongo {} failed, temporarily switching to SQL-only mode for {} ms.",
                    operation,
                    cooldownMillis,
                    exception
            );
            return;
        }

        log.debug("Mongo {} failed while SQL-only fallback is active.", operation, exception);
    }

    private long normalizeRetryAfterMillis() {
        if (retryAfter == null || retryAfter.isNegative() || retryAfter.isZero()) {
            return DEFAULT_RETRY_AFTER.toMillis();
        }
        return retryAfter.toMillis();
    }
}
