package com.eitraz.library;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeoutHandlerTest {
    public static final String MY_VALUE = "myValue";

    @Test
    public void testIsReady() throws InterruptedException {
        TimeoutHandler<String> timeoutHandler = new TimeoutHandler<>(new Duration(500, TimeUnit.MILLISECONDS));

        assertTrue(timeoutHandler.isReady(MY_VALUE));
        assertFalse(timeoutHandler.isReady(MY_VALUE));

        Thread.sleep(750);

        assertTrue(timeoutHandler.isReady(MY_VALUE));
        assertFalse(timeoutHandler.isReady(MY_VALUE));
    }

    @Test
    public void testClean() throws InterruptedException {
        TimeoutHandler<String> timeoutHandler = new TimeoutHandler<>(new Duration(2, TimeUnit.MILLISECONDS));

        assertTrue(timeoutHandler.isReady(MY_VALUE));

        Thread.sleep(750);

        assertTrue(timeoutHandler.isReady(MY_VALUE));
    }
}