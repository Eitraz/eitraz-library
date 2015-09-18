package com.eitraz.hazelcast;

import com.eitraz.lifecycle.Startable;
import com.eitraz.lifecycle.Stopable;
import com.hazelcast.core.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class HazelcastProxy<T extends Serializable> implements Startable, Stopable, MessageListener<T> {
    private final ITopic<T> topic;
    private String listenerId;

    public HazelcastProxy(String topicName) {
        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
        topic = hazelcast.getTopic(topicName);
    }

    @SuppressWarnings("unchecked")
    public <O> O proxy(final O object) {
        return (O) Proxy.newProxyInstance(object.getClass().getClassLoader(), new Class<?>[]{object.getClass()},
                (proxy, method, args) -> {
                    T event = createObjectEvent(object, method, args);

                    // Send to topic
                    if (event != null) {
                        topic.publish(createObjectEvent(object, method, args));

                        // Void
                        if (isReturnTypeVoid(method)) {
                            return Void.TYPE;
                        }
                        // Null
                        else {
                            return null;
                        }
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
    public void onMessage(Message<T> message) {
        newObjectEvent(message.getMessageObject());
    }

    /**
     * @return object event or null if it should not be distributed to other nodes
     */
    protected abstract T createObjectEvent(Object object, Method method, Object[] args);

    public abstract void newObjectEvent(T event);

    /**
     * @return true if return type of the method is 'void'
     */
    public static boolean isReturnTypeVoid(Method method) {
        return method.getReturnType().equals(Void.TYPE);
    }
}
