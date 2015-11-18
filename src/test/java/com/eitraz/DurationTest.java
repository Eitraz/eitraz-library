package com.eitraz;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class DurationTest {
    @Test
    public void testGet() throws Exception {
        Duration duration = new Duration(3, TimeUnit.SECONDS);
        assertEquals(3, duration.getValue());
        assertEquals(TimeUnit.SECONDS, duration.getUnit());
    }

    @Test
    public void testToNanos() throws Exception {
        assertEquals(1000 * 1000 * 1000, Duration.ONE_SECOND.toNanos());
    }

    @Test
    public void testToMicros() throws Exception {
        assertEquals(1000 * 1000, Duration.ONE_SECOND.toMicros());
    }

    @Test
    public void testToMillis() throws Exception {
        assertEquals(1000, Duration.ONE_SECOND.toMillis());
        assertEquals(2 * 1000, Duration.TWO_SECOND.toMillis());
        assertEquals(5 * 1000, Duration.FIVE_SECONDS.toMillis());
        assertEquals(10 * 1000, Duration.TEN_SECONDS.toMillis());
    }

    @Test
    public void testToSeconds() throws Exception {
        assertEquals(60, Duration.ONE_MINUTE.toSeconds());
        assertEquals(2 * 60, Duration.TWO_MINUTES.toSeconds());
        assertEquals(5 * 60, Duration.FIVE_MINUTES.toSeconds());
        assertEquals(10 * 60, Duration.TEN_MINUTES.toSeconds());
    }

    @Test
    public void testToMinues() throws Exception {
        assertEquals(60, Duration.ONE_HOUR.toMinutes());
        assertEquals(2 * 60, Duration.TWO_HOUR.toMinutes());
    }

    @Test
    public void testToHours() throws Exception {
        assertEquals(24, Duration.ONE_DAY.toHours());
    }

    @Test
    public void testToDays() throws Exception {
        assertEquals(5, new Duration(5, TimeUnit.DAYS).toDays());
    }
}