package com.eitraz.lifecycle;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LifecycleHandler {
    private static final Logger logger = Logger.getLogger(LifecycleHandler.class);
    public static final int JOIN_TIMEOUT = 5000;

    private Set<Object> objects = new HashSet<>();
    private Map<Object, Thread> threads = new HashMap<>();

    public <T> T register(T object) {
        objects.add(object);
        return object;
    }

    public void start() {
        for (Object object : objects) {
            String name = object.getClass().getSimpleName();

            if (object instanceof Startable) {
                logger.info(String.format("Starting '%s'", name));
                ((Startable) object).doStart();
                logger.info(String.format("'%s' started", name));
            }

            if (object instanceof Runnable) {
                logger.info(String.format("Starting thread for '%s'", name));
                Thread thread = new Thread((Runnable) object, name);
                threads.put(object, thread);
                thread.start();
                logger.info(String.format("Thread '%s' started", thread.getName()));
            }
        }
    }

    public void stop() {
        for (Object object : objects) {
            String name = object.getClass().getSimpleName();

            if (object instanceof Stopable) {
                logger.info(String.format("Stopping '%s'", name));
                ((Stopable) object).doStop();
                logger.info(String.format("'%s' stopped", name));
            }

            if (object instanceof Runnable) {
                Thread thread = threads.remove(object);
                if (thread != null) {
                    try {
                        logger.info(String.format("Waiting for thread '%s' to stop", thread.getName()));
                        thread.join(JOIN_TIMEOUT);
                        logger.info(String.format("Thread '%s' stopped", thread.getName()));
                    } catch (InterruptedException e) {
                        logger.warn(String.format("Interrupted while waiting for thread '%s'", thread.getName()), e);
                    }
                }
            }
        }
    }
}
