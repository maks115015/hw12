Sentinel:
Redis sentinel configuration locate in /sentinel, you can run it using docker compose.
TryEvictionStrategies.java - sandbox to play with eviction strategies.

Probabilistic cache:
ProbabilisticCacheJedisWrapper - class which implements probabilistic cache using Redis and Jedis library, basically wrapper for Jedis.
You can see how it works by running main method in RunProbabilisticCache.java. 
Also you can adjust BETA value to choose between more and less often cache recount, but it can cause more cache misses.