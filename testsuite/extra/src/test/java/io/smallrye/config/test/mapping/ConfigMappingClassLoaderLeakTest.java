package io.smallrye.config.test.mapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithDefault;

/**
 * A {@link ClassValue} stores its entries in the key {@link Class} own {@code classValueMap}, and the entry value is
 * held strongly. Referencing an object loaded by SmallRye Config on a JDK {@link Class} keeps the SmallRye Config
 * {@link ClassLoader} alive for as long as the JDK class, which is forever, and pins every deployment that has its own
 * copy of SmallRye Config.
 * <p>
 * Both metadata caches are probed with arbitrary types to decide whether a type is a configuration type:
 * {@code ConfigMappingInterface} for every property raw type, and {@code ConfigMappingClass} for every field type. The
 * probe must not leave anything of ours behind on the types it rejects.
 */
class ConfigMappingClassLoaderLeakTest {
    @Test
    void noReferencesOnJdkClasses() {
        new SmallRyeConfigBuilder()
                .withMapping(ReferenceMapping.class)
                .withMapping(ReferenceClass.class)
                .build();

        Map<String, List<String>> references = new LinkedHashMap<>();
        for (Class<?> jdkType : List.of(String.class, int.class, Integer.class, Duration.class, List.class)) {
            List<String> reference = references(jdkType);
            if (!reference.isEmpty()) {
                references.put(jdkType.getName(), reference);
            }
        }

        assertTrue(references.isEmpty(), () -> "SmallRye Config objects references on JDK classes: " + references);
    }

    @SuppressWarnings("unchecked")
    private static List<String> references(final Class<?> type) {
        List<String> references = new ArrayList<>();
        try {
            Field classValueMap = Class.class.getDeclaredField("classValueMap");
            classValueMap.setAccessible(true);
            Map<Object, Object> entries = (Map<Object, Object>) classValueMap.get(type);
            if (entries == null) {
                return references;
            }

            Field entryValue = Class.forName("java.lang.ClassValue$Entry").getDeclaredField("value");
            entryValue.setAccessible(true);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                Object value = entryValue.get(entry.getValue());
                if (value != null && value.getClass().getName().startsWith("io.smallrye.config.")) {
                    references.add(value.getClass().getName());
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot inspect " + type.getName() + " classValueMap", e);
        }
        return references;
    }

    @ConfigMapping
    public interface ReferenceMapping {
        @WithDefault("host")
        String host();

        @WithDefault("8080")
        int port();

        @WithDefault("PT10S")
        Duration timeout();
    }

    public static class ReferenceClass {
        @WithDefault("host")
        String host;
        @WithDefault("8080")
        int port;
        @WithDefault("PT10S")
        Duration timeout;
    }
}
