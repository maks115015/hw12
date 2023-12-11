package org.example;

import java.time.Clock;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

@RequiredArgsConstructor
public class ProbabilisticCacheJedisWrapper {

    private static final Random RANDOM = new java.util.Random();
    private static final String VALUE_KEY = "VALUE";
    private static final String LAST_DELTA = "DELTA";
    private static final Clock clock = Clock.systemUTC();

    private final AtomicLong cacheHit;
    private final AtomicLong cacheMiss;
    private final AtomicLong cacheProbabilisticRecount;
    @Delegate
    private final Jedis jedis;

    public String getValue(String key, int ttlSeconds, int beta) {

        Transaction transaction = multi();
        Response<Map<String, String>> resultMapResponse = transaction.hgetAll(key);
        Response<Long> ttl = transaction.ttl(key);
        transaction.exec();
        transaction.close();

        Map<String, String> resultMap = resultMapResponse.get();
        long remainTTlSeconds = ttl.get();
        String value = resultMap.get(VALUE_KEY);

        if (value == null) {
            System.out.println("Cache miss, total count: " + cacheMiss.incrementAndGet());
            return recountCache(key, ttlSeconds);
        } else if (shouldRecompute(getLastDelta(resultMap), remainTTlSeconds, beta)) {
            System.out.println("Cache probabilistic recount, total count: " + cacheProbabilisticRecount.incrementAndGet());
            return recountCache(key, ttlSeconds);
        } else {
            System.out.println("Cache hit, total count: " + cacheHit.incrementAndGet());
            return value;
        }
    }

    private String recountCache(String key, int ttlSeconds) {
        long start = clock.millis();
        String newVal = computeNewValue();
        long newDeltaSeconds = (clock.millis() - start) / 1000;

        save(key, newVal, newDeltaSeconds, ttlSeconds);

        return newVal;
    }

    private long getLastDelta(Map<String, String> resultMap) {
        return Long.parseLong(resultMap.get(LAST_DELTA));
    }

    private boolean shouldRecompute(long lastDeltaSeconds, long remainTTlSeconds, int beta) {
        float random = RANDOM.nextFloat(1);
        double xFetch = lastDeltaSeconds * beta * Math.log(random);
        long currentTimestampInSeconds = System.currentTimeMillis() / 1000;
        long cacheExpiresAt = currentTimestampInSeconds + remainTTlSeconds;

        return (currentTimestampInSeconds - xFetch) >= cacheExpiresAt;
    }

    private void save(String key, String newVal, long newDeltaSeconds, int newTtlSeconds) {
        Map<String, String> newValues = Map.of(VALUE_KEY, newVal, LAST_DELTA, String.valueOf(newDeltaSeconds));

        Transaction transaction = multi();
        transaction.hmset(key, newValues);
        transaction.expire(key, newTtlSeconds);
        transaction.exec();
        transaction.close();
    }

    @SneakyThrows
    private String computeNewValue() {
        Thread.sleep(RANDOM.nextLong(500, 3000)); // imitate long computation
        return clock.millis() + UUID.randomUUID().toString();
    }
}
