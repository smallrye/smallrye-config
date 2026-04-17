package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.DefaultValuesConfigSource.Defaults;

class DefaultValuesConfigSourceTest {
    @Test
    void wildcardNames() {
        Map<String, String> wildcards = new HashMap<>();
        wildcards.put("a.wildcard.*", "value");

        Defaults defaults = new Defaults();
        defaults.put(wildcards);

        DefaultValuesConfigSource defaultValuesConfigSource = new DefaultValuesConfigSource(defaults);
        assertTrue(defaultValuesConfigSource.getPropertyNames().isEmpty());

        defaultValuesConfigSource = new DefaultValuesConfigSource(wildcards, "Defaults", 100);
        assertTrue(defaultValuesConfigSource.getPropertyNames().isEmpty());
    }
}
