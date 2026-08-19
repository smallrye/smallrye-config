package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappings.ConfigClass;

class ConfigMappingHandlerTest {
    @Test
    void overrideHandler() {
        ConfigMappingHandler overrideHandler = new ConfigMappingHandler() {
            @Override
            public boolean handles(Class<?> type) {
                return type.isInterface() && type.isAnnotationPresent(ConfigMapping.class);
            }

            @Override
            public String getPrefix(Class<?> type) {
                return "override";
            }
        };

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("override.value", "value")
                .withDefaultValue("override.anotherValue", "value")
                .withDefaultValue("override.nested.nestedValue", "value")
                .withMapping(ConfigClass.configClass(OverrideHandler.class, overrideHandler))
                .build();

        OverrideHandler mapping = config.getConfigMapping(OverrideHandler.class, "override");
        assertEquals("value", mapping.value());
        assertEquals("value", mapping.anotherValue());
    }

    @ConfigMapping(prefix = "something")
    interface OverrideHandler {
        String value();

        String anotherValue();

        Nested nested();

        interface Nested {
            String nestedValue();
        }
    }

    @Test
    void sharedNestedWithDifferentHandlers() {
        ConfigMappingHandler handlerA = new ConfigMappingHandler() {
            @Override
            public boolean handles(Class<?> type) {
                return true;
            }

            @Override
            public String getPrefix(Class<?> type) {
                return "root-a";
            }
        };

        ConfigMappingHandler handlerB = new ConfigMappingHandler() {
            @Override
            public boolean handles(Class<?> type) {
                return true;
            }

            @Override
            public String getPrefix(Class<?> type) {
                return "root-b";
            }
        };

        ConfigMappingLoader.getGeneratedConfigClasses(RootA.class, handlerA);
        assertDoesNotThrow(() -> ConfigMappingLoader.getGeneratedConfigClasses(RootB.class, handlerB));
    }

    static class RootA {
        SharedNested nested;
    }

    static class RootB {
        SharedNested nested;
    }

    static class SharedNested {
        String value;
    }
}
