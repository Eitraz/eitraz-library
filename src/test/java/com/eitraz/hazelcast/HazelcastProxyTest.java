package com.eitraz.hazelcast;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HazelcastProxyTest {
    private Map<String, Object> objects = new ConcurrentHashMap<>();
    private HazelcastProxy proxy;

    @Before
    public void before() {
        proxy = getHazelcastProxy();
        proxy.doStart();
    }

    @After
    public void after() {
        if (proxy != null) {
            proxy.doStop();
            proxy = null;
        }
    }

    public TestInterface proxy(TestObject object) {
        return proxy.proxy(object, TestInterface.class);
    }

    @Test
    public void testReturnValue() throws Exception {
        TestObject a = registerObject("a", new TestObject());
        assertEquals(new Integer(1), proxy(a).increase());
    }

    @Test
    public void testDoNotReturnValue() throws Exception {
        proxy.setReturnValue(false);

        TestObject a = registerObject("a", new TestObject());
        assertNull(proxy(a).increase());
        Thread.sleep(1000);
        assertEquals(new Integer(1), a.getValue());
    }

    @Test
    public void testSetValue() throws Exception {
        TestObject a = registerObject("a", new TestObject());
        proxy(a).setValue(2);
        Thread.sleep(1000);
        assertEquals(new Integer(2), a.getValue());
    }

    private <O> O registerObject(String reference, O object) {
        objects.put(reference, object);
        return object;
    }

    private HazelcastProxy getHazelcastProxy() {
        return new HazelcastProxy("test-topic") {
            @Override
            protected String createObjectReference(Object object) {
                for (Map.Entry<String, Object> entry : objects.entrySet()) {
                    if (entry.getValue() == object)
                        return entry.getKey();
                }
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <O> O createObjectFromReference(String reference) {
                return (O) objects.get(reference);
            }
        };
    }

    public interface TestInterface {
        Integer increase();

        Integer decrease();

        Integer getValue();

        void setValue(Integer value);
    }

    public static class TestObject implements TestInterface {
        private int value = 0;

        @Override
        public synchronized Integer increase() {
            return ++value;
        }

        @Override
        public synchronized Integer decrease() {
            return --value;
        }

        @Override
        public void setValue(Integer value) {
            this.value = value;
        }

        @Override
        public Integer getValue() {
            return value;
        }
    }
}