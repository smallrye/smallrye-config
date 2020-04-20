package io.smallrye.config.examples.interceptors;

import static org.junit.Assert.*;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.Test;

public class ExampleInterceptorTest {
    @Test
    public void getValue() {
        final String myProp = ConfigProvider.getConfig().getValue("my.prop", String.class);
        assertEquals("intercepted 1", myProp);
    }
}
