package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

@ExtendWith(WeldJunit5Extension.class)
public class MapInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MapBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    MapBean mapBean;

    @Test
    void map() {
        assertEquals(3, mapBean.getMap().size());
        assertEquals("value", mapBean.getMap().get("key"));
        assertEquals("value", mapBean.getMap().get("key.nested"));
        assertEquals("value", mapBean.getMap().get("key.quoted"));
        assertEquals("value", mapBean.getDefaults().get("default"));
        assertEquals("value", mapBean.getConverted().get(new Key("key")).getValue());
    }

    @Test
    void optionals() {
        assertFalse(mapBean.getOptionalEmpty().isPresent());
        assertTrue(mapBean.getOptionalDefaults().isPresent());
        assertEquals("value", mapBean.getDefaults().get("default"));
    }

    @ApplicationScoped
    static class MapBean {
        @Inject
        @ConfigProperty(name = "map", defaultValue = "default=value")
        Map<String, String> map;
        @Inject
        @ConfigProperty(name = "map.defaults", defaultValue = "default=value")
        Map<String, String> defaults;
        @Inject
        @ConfigProperty(name = "converted")
        Map<Key, Value> converted;
        @Inject
        @ConfigProperty(name = "optionals.empty")
        Optional<Map<String, String>> optionalEmpty;
        @Inject
        @ConfigProperty(name = "optionals.defaults", defaultValue = "default=value")
        Optional<Map<String, String>> optionalDefaults;

        Map<String, String> getMap() {
            return map;
        }

        Map<String, String> getDefaults() {
            return defaults;
        }

        Map<Key, Value> getConverted() {
            return converted;
        }

        Optional<Map<String, String>> getOptionalEmpty() {
            return optionalEmpty;
        }

        Optional<Map<String, String>> getOptionalDefaults() {
            return optionalDefaults;
        }
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("map.key", "value", "map.key.nested", "value", "map.\"key.quoted\"", "value"))
                .withSources(config("converted.key", "value"))
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }

    static class Key {
        private final String key;

        public Key(final String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Key key1 = (Key) o;
            return key.equals(key1.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    static class Value {
        private final String value;

        public Value(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
