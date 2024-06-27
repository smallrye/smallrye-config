package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

class ConfigMappingEqualsTest {
    @Test
    void equals() {
        MappingEquals o1 = config().getConfigMapping(MappingEquals.class);
        MappingEquals o2 = config("sets.nested[0].value", "bar", "sets.nested[1].value", "foo")
                .getConfigMapping(MappingEquals.class);
        assertEquals(o1.getClass(), o2.getClass());
        assertEquals(o1.prim().getClass(), o2.prim().getClass());
        assertEquals(o1, o1);
        assertEquals(o2, o2);
        assertEquals(o1, o2);
        assertEquals(o2, o1);
        assertEquals(o1.hashCode(), o2.hashCode());

        assertEquals(o1.prim(), o2.prim());
        assertEquals(o1.lists(), o2.lists());
        assertEquals(o1.sets(), o2.sets());
        assertEquals(o1.optionals(), o2.optionals());
        assertEquals(o1.maps(), o2.maps());
    }

    @Test
    void notEqualsPrimitives() {
        MappingEquals o1 = config().getConfigMapping(MappingEquals.class);
        MappingEquals o2 = config("prim.z", "true").getConfigMapping(MappingEquals.class);
        assertEquals(o1.getClass(), o2.getClass());
        assertEquals(o1.prim().getClass(), o2.prim().getClass());
        assertNotEquals(o1, o2);
        assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void notEqualsLists() {
        MappingEquals o1 = config().getConfigMapping(MappingEquals.class);
        MappingEquals o2 = config("lists.nested[0].value", "baz").getConfigMapping(MappingEquals.class);
        assertEquals(o1.getClass(), o2.getClass());
        assertEquals(o1.prim().getClass(), o2.prim().getClass());
        assertNotEquals(o1, o2);
        assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void notEqualsOptionals() {
        MappingEquals o1 = config().getConfigMapping(MappingEquals.class);
        MappingEquals o2 = config("optionals.empty", "value").getConfigMapping(MappingEquals.class);
        assertEquals(o1.getClass(), o2.getClass());
        assertEquals(o1.prim().getClass(), o2.prim().getClass());
        assertNotEquals(o1, o2);
        assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void notEqualsMaps() {
        MappingEquals o1 = config().getConfigMapping(MappingEquals.class);
        MappingEquals o2 = config("maps.list-nested.key[1].value", "value").getConfigMapping(MappingEquals.class);
        assertEquals(o1.getClass(), o2.getClass());
        assertEquals(o1.prim().getClass(), o2.prim().getClass());
        assertNotEquals(o1, o2);
        assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    private static SmallRyeConfig config(final String... overrides) {
        return new SmallRyeConfigBuilder()
                .withMapping(MappingEquals.class)
                .withSources(KeyValuesConfigSource.config(overrides))
                .withSources(MappingEquals.getDefaults())
                .build();
    }

    @ConfigMapping
    interface MappingEquals {
        Primitives prim();

        Lists lists();

        Sets sets();

        Optionals optionals();

        Maps maps();

        interface Primitives {
            @WithDefault("false")
            boolean z();

            @WithDefault("c")
            char c();

            @WithDefault("0")
            byte b();

            @WithDefault("1")
            int i();

            @WithDefault("10")
            short s();

            @WithDefault(Long.MAX_VALUE + "")
            long l();

            @WithDefault("0.1f")
            float f();

            @WithDefault(Double.MAX_VALUE + "")
            double d();
        }

        interface Lists {
            List<String> simple();

            List<Nested> nested();

            interface Nested {
                String value();
            }
        }

        interface Sets {
            Set<String> simple();

            Set<Nested> nested();

            interface Nested {
                String value();
            }
        }

        interface Optionals {
            Optional<String> empty();

            Optional<String> simple();

            Optional<Nested> nested();

            interface Nested {
                String value();
            }
        }

        interface Maps {
            Map<String, String> simple();

            Map<String, Nested> nested();

            Map<String, List<String>> list();

            Map<String, List<Nested>> listNested();

            interface Nested {
                String value();
            }
        }

        static ConfigSource getDefaults() {
            return KeyValuesConfigSource.config(
                    "config_ordinal", "0",
                    "lists.simple", "foo,bar",
                    "lists.nested[0].value", "foo",
                    "lists.nested[1].value", "bar",
                    "sets.simple", "foo,bar",
                    "sets.nested[0].value", "foo",
                    "sets.nested[1].value", "bar",
                    "optionals.simple", "value",
                    "optionals.nested.value", "value",
                    "maps.simple.key", "value",
                    "maps.nested.key.value", "value",
                    "maps.list.key[0]", "value",
                    "maps.list-nested.key[0].value", "value");
        }
    }
}
