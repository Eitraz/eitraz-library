package com.eitraz.library.lifecycle;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LifecycleHandlerTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private LifecycleHandler handler = new LifecycleHandler();

    public LifecycleHandlerTest() {
        Awaitility.setDefaultTimeout(Duration.ONE_SECOND);
    }

    @Test
    public void testStartable() throws Exception {
        OnlyStartable startable = handler.register(new OnlyStartable());
        assertFalse(startable.started.get());

        handler.start();
        assertTrue(startable.started.get());

        handler.stop();
        assertTrue(startable.started.get());
    }

    @Test
    public void testStopable() throws Exception {
        OnlyStopable stopable = handler.register(new OnlyStopable());
        assertFalse(stopable.stopped.get());

        handler.start();
        assertFalse(stopable.stopped.get());

        handler.stop();
        assertTrue(stopable.stopped.get());
    }

    @Test
    public void testStartableAndStopable() throws Exception {
        StartableAndStopable startAndStopable = handler.register(new StartableAndStopable());
        assertFalse(startAndStopable.started.get());
        assertFalse(startAndStopable.stopped.get());

        handler.start();
        assertTrue(startAndStopable.started.get());
        assertFalse(startAndStopable.stopped.get());

        handler.stop();
        assertTrue(startAndStopable.started.get());
        assertTrue(startAndStopable.stopped.get());
    }

    @Test
    public void testRunnable() throws Exception {
        OnlyRunnable runnable = handler.register(new OnlyRunnable());
        assertFalse(runnable.hasRun.get());

        handler.start();
        await().untilTrue(runnable.hasRun);
        assertTrue(runnable.hasRun.get());

        handler.stop();
        assertTrue(runnable.hasRun.get());
    }

    @Test
    public void testStartableAndRunnable() throws Exception {
        StartableAndRunnable startAndRunnable = handler.register(new StartableAndRunnable());
        assertFalse(startAndRunnable.started.get());
        assertFalse(startAndRunnable.hasRun.get());

        handler.start();
        assertTrue(startAndRunnable.started.get());

        handler.stop();
        assertTrue(startAndRunnable.started.get());
        await().untilTrue(startAndRunnable.hasRun);
    }

    @Test
    public void RunnableAndStopable() throws Exception {
        RunnableAndStopable runnableAndStopable = handler.register(new RunnableAndStopable());
        assertFalse(runnableAndStopable.hasRun.get());
        assertFalse(runnableAndStopable.stopped.get());

        handler.start();
        assertFalse(runnableAndStopable.stopped.get());

        handler.stop();
        await().untilTrue(runnableAndStopable.hasRun);
        assertTrue(runnableAndStopable.stopped.get());
    }

    @Test
    public void StartableRunnableAndStopable() throws Exception {
        StartableRunnableAndStopable startableRunnableAndStopable = handler.register(new StartableRunnableAndStopable());
        assertFalse(startableRunnableAndStopable.started.get());
        assertFalse(startableRunnableAndStopable.hasRun.get());
        assertFalse(startableRunnableAndStopable.stopped.get());

        handler.start();
        assertTrue(startableRunnableAndStopable.started.get());
        assertFalse(startableRunnableAndStopable.stopped.get());

        handler.stop();
        assertTrue(startableRunnableAndStopable.started.get());
        await().untilTrue(startableRunnableAndStopable.hasRun);
        assertTrue(startableRunnableAndStopable.stopped.get());
    }

    public class OnlyStartable implements Startable {
        private AtomicBoolean started = new AtomicBoolean(false);

        @Override
        public void doStart() {
            started.set(true);
        }
    }

    public class OnlyStopable implements Stopable {
        private AtomicBoolean stopped = new AtomicBoolean(false);

        @Override
        public void doStop() {
            stopped.set(true);
        }
    }

    public class StartableAndStopable implements Startable, Stopable {
        private AtomicBoolean started = new AtomicBoolean(false);
        private AtomicBoolean stopped = new AtomicBoolean(false);

        @Override
        public void doStart() {
            started.set(true);
        }

        @Override
        public void doStop() {
            stopped.set(true);
        }
    }

    public class OnlyRunnable implements Runnable {
        private AtomicBoolean hasRun = new AtomicBoolean(false);

        @Override
        public void run() {
            hasRun.set(true);
        }
    }

    public class StartableAndRunnable implements Startable, Runnable {
        private AtomicBoolean started = new AtomicBoolean(false);
        private AtomicBoolean hasRun = new AtomicBoolean(false);

        @Override
        public void doStart() {
            started.set(true);
        }

        @Override
        public void run() {
            hasRun.set(true);
        }
    }

    public class RunnableAndStopable implements Runnable, Stopable {
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private AtomicBoolean hasRun = new AtomicBoolean(false);

        @Override
        public void run() {
            while (!stopped.get()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }

            hasRun.set(true);
        }

        @Override
        public void doStop() {
            stopped.set(true);
        }
    }

    public class StartableRunnableAndStopable implements Startable, Runnable, Stopable {
        private AtomicBoolean started = new AtomicBoolean(false);
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private AtomicBoolean hasRun = new AtomicBoolean(false);

        @Override
        public void doStart() {
            started.set(true);
        }

        @Override
        public void run() {
            while (started.get() && !stopped.get()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            hasRun.set(true);
        }

        @Override
        public void doStop() {
            stopped.set(true);
        }
    }
}