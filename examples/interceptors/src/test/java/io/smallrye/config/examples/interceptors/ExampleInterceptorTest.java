package io.smallrye.config.examples.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

class ExampleInterceptorTest {
    @Test
    void getValue() {
        final String myProp = ConfigProvider.getConfig().getValue("my.prop", String.class);
        assertEquals("intercepted 1", myProp);
    }
}
