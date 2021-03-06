package com.eitraz.library;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TimeoutHandler<T> {
    private static final Logger logger = Logger.getLogger(TimeoutHandler.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ONE_SECOND;

    private static final int MAX_CLEAN_TIMEOUT = 600000;
    private static final long CLEAN_TIMEOUT_MULTIPLIER = 60 * 5;

    private long cacheClearTime = System.currentTimeMillis();
    private final Map<T, Long> cache;

    private Duration timeout;

    public TimeoutHandler() {
        this(DEFAULT_TIMEOUT);
    }

    public TimeoutHandler(Duration timeout) {
        this(timeout, new ConcurrentHashMap<>());
    }

    public TimeoutHandler(Duration timeout, Map<T, Long> cache) {
        setTimeout(timeout);
        this.cache = cache;
    }

    /**
     * @return the timeout
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * @return true if value is timed out
     */
    public synchronized boolean isReady(T value) {
        // Clean up values left behind
        clean();

        // Get timeout value
        Long timeout = cache.get(value);

        long time = System.currentTimeMillis();

        // Timed out
        if (timeout != null && timeout < time) {
            timeout = null;
        }

        if (logger.isTraceEnabled())
            logger.trace(String.format("'%s' timed out: %s", value, timeout == null));

        // Set timeout value
        cache.put(value, time + getTimeout().toMillis());

        // Return true if value is timed out
        return timeout == null;
    }

    /**
     * Clean up values left behind
     */
    private void clean() {
        long time = System.currentTimeMillis();

        // Don't clean to often
        if (cacheClearTime + Math.min(getTimeout().toMillis() * CLEAN_TIMEOUT_MULTIPLIER, MAX_CLEAN_TIMEOUT) < time) {
            if (logger.isDebugEnabled())
                logger.debug("Cleaning cache (values before clean: " + cache.size());

            // Remove if timed out
            cache.entrySet().stream()
                    .filter(entry -> entry.getValue() <= time)
                    .forEach(entry -> cache.remove(entry.getKey()));

            if (logger.isDebugEnabled())
                logger.debug("Values after clean: " + cache.size());

            cacheClearTime = time;
        }
    }

}
