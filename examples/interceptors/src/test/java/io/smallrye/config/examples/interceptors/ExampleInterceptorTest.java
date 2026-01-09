package io.smallrye.config.examples.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.smallrye.config.Config;

class ExampleInterceptorTest {
    @Test
    void getValue() {
        String myProp = Config.getOrCreate().getValue("my.prop", String.class);
        assertEquals("intercepted 1", myProp);
    }
}
