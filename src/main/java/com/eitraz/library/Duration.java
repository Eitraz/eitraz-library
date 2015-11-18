package com.eitraz.library;

import java.util.concurrent.TimeUnit;

public class Duration {
    public static final Duration ONE_SECOND = new Duration(1, TimeUnit.SECONDS);
    public static final Duration TWO_SECOND = new Duration(2, TimeUnit.SECONDS);
    public static final Duration FIVE_SECONDS = new Duration(5, TimeUnit.SECONDS);
    public static final Duration TEN_SECONDS = new Duration(10, TimeUnit.SECONDS);

    public static final Duration ONE_MINUTE = new Duration(1, TimeUnit.MINUTES);
    public static final Duration TWO_MINUTES = new Duration(2, TimeUnit.MINUTES);
    public static final Duration FIVE_MINUTES = new Duration(5, TimeUnit.MINUTES);
    public static final Duration TEN_MINUTES = new Duration(10, TimeUnit.MINUTES);

    public static final Duration ONE_HOUR = new Duration(1, TimeUnit.HOURS);
    public static final Duration TWO_HOUR = new Duration(2, TimeUnit.HOURS);

    public static final Duration ONE_DAY = new Duration(1, TimeUnit.DAYS);

    private final long value;
    private final TimeUnit unit;

    public Duration(long value, TimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public long getValue() {
        return value;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public long toNanos() {
        return unit.toNanos(value);
    }

    public long toMicros() {
        return unit.toMicros(value);
    }

    public long toMillis() {
        return unit.toMillis(value);
    }

    public long toSeconds() {
        return unit.toSeconds(value);
    }

    public long toMinutes() {
        return unit.toMinutes(value);
    }

    public long toHours() {
        return unit.toHours(value);
    }

    public long toDays() {
        return unit.toDays(value);
    }
}
