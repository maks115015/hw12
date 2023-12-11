package org.example;

import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class ExtendedJedisPool extends JedisPool {

    @Getter
    private final AtomicLong cacheHit = new AtomicLong();
    @Getter
    private final AtomicLong cacheMiss = new AtomicLong();
    @Getter
    private final AtomicLong cacheProbabilisticRecount = new AtomicLong();

    public ExtendedJedisPool(GenericObjectPoolConfig<Jedis> poolConfig, String host, int port) {
        super(poolConfig, host, port);
    }

    public ProbabilisticCacheJedisWrapper getXFetchResource() {
        Jedis jedis = this.getResource();

        return new ProbabilisticCacheJedisWrapper(cacheHit, cacheMiss, cacheProbabilisticRecount, jedis);
    }
}