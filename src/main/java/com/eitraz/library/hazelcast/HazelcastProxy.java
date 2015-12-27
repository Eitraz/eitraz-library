package com.eitraz.library.hazelcast;

import com.eitraz.library.lifecycle.Startable;
import com.eitraz.library.lifecycle.Stopable;
import com.hazelcast.core.*;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class HazelcastProxy implements Startable, Stopable, MessageListener<HazelcastProxy.MethodCall> {
    private static final Logger logger = Logger.getLogger(HazelcastProxy.class);

    private final HazelcastInstance hazelcast;
    private final ITopic<MethodCall> topic;
    private boolean returnValue = true;
    private String listenerId;

    private BlockingQueue<MethodCall> methodCalls = new LinkedBlockingQueue<>();
    private Thread invoker;

    public HazelcastProxy(HazelcastInstance hazelcast, String topicName) {
        this.hazelcast = hazelcast;
        topic = hazelcast.getTopic(topicName);
    }

    /**
     * @return true if the proxy runs and return the value locally
     */
    public boolean isReturnValue() {
        return returnValue;
    }

    /**
     * @param returnValue true if the proxy should run and return the value locally
     */
    public void setReturnValue(boolean returnValue) {
        this.returnValue = returnValue;
    }

    @SuppressWarnings("unchecked")
    public <O> O proxy(final O object, Class<O> type) {
        return (O) Proxy.newProxyInstance(object.getClass().getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    String objectReference = createObjectReference(object);

                    // Send to topic
                    if (objectReference != null) {
                        Object ret;
                        boolean executeLocally;

                        // Void
                        if (isReturnTypeVoid(method)) {
                            ret = Void.TYPE;
                            executeLocally = true;
                        }
                        // Invoke method local and return value
                        else if (isReturnValue()) {
                            ret = method.invoke(object, args);
                            executeLocally = false;
                        }
                        // Return null - only invoke async
                        else {
                            ret = null;
                            executeLocally = true;
                        }

                        // Publish
                        topic.publish(new MethodCall(objectReference, method, args, executeLocally));

                        return ret;
                    }
                    // Call once and return result
                    else {
                        return method.invoke(object, args);
                    }
                });
    }

    @Override
    public synchronized void doStart() {
        if (listenerId == null) {
            listenerId = topic.addMessageListener(this);
        }

        if (invoker == null) {
            invoker = new Thread("HazelcastProxy invoker") {
                @Override
                public void run() {
                    logger.info("Started");

                    while (invoker == this) {
                        try {
                            MethodCall methodCall = methodCalls.poll(1, TimeUnit.SECONDS);

                            // Execute
                            if (methodCall != null)
                                invoke(methodCall);

                        } catch (InterruptedException e) {
                            logger.error(e);
                        } catch (HazelcastInstanceNotActiveException ignored) {
                            break;
                        }
                    }

                    logger.info("Stopped");
                }
            };
            invoker.start();
        }
    }

    @Override
    public synchronized void doStop() {
        invoker = null;

        if (listenerId != null) {
            topic.removeMessageListener(listenerId);
            listenerId = null;
        }
    }

    @Override
    public void onMessage(Message<MethodCall> message) {
        MethodCall methodCall = message.getMessageObject();

        // Don't invoke locally
        if (!methodCall.isExecuteLocally() && message.getPublishingMember().equals(hazelcast.getCluster().getLocalMember()))
            return;

        methodCalls.offer(methodCall);
    }

    /**
     * @param methodCall method call to invoke
     */
    private void invoke(MethodCall methodCall) {
        String objectReference = methodCall.getObjectReference();

        Object object = createObjectFromReference(objectReference);
        if (object == null) {
            logger.error(String.format("Failed to create object for reference '%s'", objectReference));
            return;
        }

        String methodName = methodCall.getMethod();
        Object[] args = methodCall.getArgs();

        List<Class<?>> parameterTypes = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                parameterTypes.add(arg.getClass());
            }
        }

        logger.info(String.format("Executing method '%s' on object '%s' with arguments %s", methodName, objectReference, Arrays.toString(args)));

        try {
            Method method = object.getClass().getMethod(
                    methodName, parameterTypes.toArray(new Class<?>[parameterTypes.size()]));

            method.invoke(object, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error(
                    String.format("Failed to invoke method '%s' with arguments '%s' on object '%s' (%s)",
                            methodName,
                            Arrays.deepToString(args),
                            objectReference,
                            object.getClass().getCanonicalName()),
                    e);
        }
    }

    protected abstract String createObjectReference(Object object);

    protected abstract <O> O createObjectFromReference(String reference);

    /**
     * @return true if return type of the method is 'void'
     */
    public static boolean isReturnTypeVoid(Method method) {
        return method.getReturnType().equals(Void.TYPE);
    }

    public static class MethodCall implements Serializable {
        private final String objectReference;
        private final String method;
        private final Object[] args;
        private final boolean executeLocally;

        public MethodCall(String objectReference, Method method, Object[] args, boolean executeLocally) {
            this.objectReference = objectReference;
            this.method = method.getName();
            this.args = args;
            this.executeLocally = executeLocally;
        }

        public String getObjectReference() {
            return objectReference;
        }

        public String getMethod() {
            return method;
        }

        public Object[] getArgs() {
            return args;
        }

        public boolean isExecuteLocally() {
            return executeLocally;
        }
    }
}
