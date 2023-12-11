package org.example;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

public class TryEvictionStrategies {

    private static final int PORT = 6379;
    private static final String HOST = "localhost";

    private static final ExecutorService executorService = Executors.newWorkStealingPool(5);
    private static final Random RANDOM = new java.util.Random();

    public static void main(String[] args) {
        final JedisPoolConfig poolConfig = buildPoolConfig();

        try (JedisPool jedisPool = new JedisPool(poolConfig, HOST, PORT)) {
            executorService.submit(() -> readValue(jedisPool));

            writeValue(jedisPool);
        }
    }

    private static JedisPoolConfig buildPoolConfig() {
        return new JedisPoolConfig();
    }

    private static void readValue(JedisPool jedisPool) {
        try (Jedis jedis = jedisPool.getResource())
        {
            while (1 < 2) {
                    printSize(jedis);
                    int random = RANDOM.nextInt(10000);
                    System.out.println("Try get key " + random + ", value:" + jedis.get(String.valueOf(RANDOM.nextInt(random))));
            }
        }
    }

    private static void writeValue(JedisPool jedisPool) {

        for (int i = 0; i < 10000000; i++) {
            try(Jedis jedis = jedisPool.getResource();) {
                printSize(jedis);
                jedis.set(String.valueOf(i), UUID.randomUUID().toString(), SetParams.setParams().ex(120));
            }
        }
    }

    private static void printSize(Jedis jedis) {
        System.out.println(jedis.keys("*").size());
    }
}