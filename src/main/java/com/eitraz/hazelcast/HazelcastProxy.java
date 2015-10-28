package com.eitraz.hazelcast;

import com.eitraz.lifecycle.Startable;
import com.eitraz.lifecycle.Stopable;
import com.hazelcast.core.*;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class HazelcastProxy implements Startable, Stopable, MessageListener<HazelcastProxy.MethodCall> {
    private static final Logger logger = Logger.getLogger(HazelcastProxy.class);

    private final HazelcastInstance hazelcast;
    private final ITopic<MethodCall> topic;
    private boolean returnValue = true;
    private String listenerId;

    public HazelcastProxy(String topicName) {
        hazelcast = Hazelcast.newHazelcastInstance();
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
                        else if (returnValue) {
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
    public void doStart() {
        if (listenerId == null) {
            listenerId = topic.addMessageListener(this);
        }
    }

    @Override
    public void doStop() {
        if (listenerId != null) {
            topic.removeMessageListener(listenerId);
            listenerId = null;
        }
    }

    @Override
    public void onMessage(Message<MethodCall> message) {
        MethodCall methodCall = message.getMessageObject();

        // Don't execute locally
        if (!methodCall.isExecuteLocally() && message.getPublishingMember().equals(hazelcast.getCluster().getLocalMember()))
            return;

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
                    String.format("Failed to execute method '%s' with arguments '%s' on object '%s' (%s)",
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
