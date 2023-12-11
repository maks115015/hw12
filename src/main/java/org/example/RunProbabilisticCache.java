package org.example;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.SneakyThrows;
import redis.clients.jedis.JedisPoolConfig;

public class RunProbabilisticCache {

    private static final int PARALLELISM = 100;
    private static final int RUNNING_TIME_SECONDS = 30;
    private static final int TTL_SECONDS = 10;
    private static final int BETA = 2; // tuned value, more - more often cache will be recounted by probability

    private static final String KEY = "key";
    private static final int PORT = 6379;
    private static final String HOST = "localhost";

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool(PARALLELISM);


    public static void main(String[] args) {

        final JedisPoolConfig poolConfig = buildPoolConfig();

        try (ExtendedJedisPool jedisPool = new ExtendedJedisPool(poolConfig, HOST, PORT))
        {

            //warmup cache
            jedisPool.getXFetchResource().getValue(KEY, TTL_SECONDS, BETA);

            //go with high load
            runTasksConcurrently(jedisPool);

            //print results
            System.out.println("---------------------------------");
            System.out.printf("""
                Cache hit: %d
                Cache miss: %d
                Cache probabilistic recount: %d
                %n""", jedisPool.getCacheHit().get(), jedisPool.getCacheMiss().get(), jedisPool.getCacheProbabilisticRecount().get());
        }
    }

    @SneakyThrows
    private static void runTasksConcurrently(ExtendedJedisPool jedisPool) {
        LocalDateTime current = LocalDateTime.now();

        while (current.plusSeconds(RUNNING_TIME_SECONDS).isAfter(LocalDateTime.now())) {
            Thread.sleep(10);      // to relax connections
            EXECUTOR_SERVICE.execute(() -> {
                ProbabilisticCacheJedisWrapper probabilisticCacheJedisWrapper = jedisPool.getXFetchResource();
                probabilisticCacheJedisWrapper.getValue(KEY, TTL_SECONDS, BETA);
                probabilisticCacheJedisWrapper.close();
            });
        }
    }

    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(256);
        poolConfig.setMaxIdle(256);
        poolConfig.setMinIdle(64);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }
}