package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.config.common.MapBackedConfigSource;

class ConfigMappingWithKeysTest {
    @Test
    void withKeys() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("", Map.of(
                        "map.leaf.one", "one",
                        "map.leaf.two", "two",
                        "map.leaf.dashed-key", "dashed-value",
                        "map.leaf.\"dotted.key\"", "dotted.value")) {
                    @Override
                    public Set<String> getPropertyNames() {
                        return Collections.emptySet();
                    }
                })
                .withSources(new MapBackedConfigSource("", Map.of(
                        "map.nested.one.value", "one",
                        "map.nested.two.value", "two",
                        "map.nested.dashed-key.value", "dashed-value",
                        "map.nested.\"dotted.key\".value", "dotted.value")) {
                    @Override
                    public Set<String> getPropertyNames() {
                        return Collections.emptySet();
                    }
                })
                .withSources(new MapBackedConfigSource("", Map.of(
                        "map.leaf-list.one[0]", "one",
                        "map.leaf-list.two[0]", "two",
                        "map.leaf-list.dashed-key[0]", "dashed-value",
                        "map.leaf-list.\"dotted.key\"[0]", "dotted.value")) {
                    @Override
                    public Set<String> getPropertyNames() {
                        // indexed properties still need to query property names, because indexes are also dynamic
                        return super.getPropertyNames();
                    }
                })
                .withSources(new MapBackedConfigSource("", Map.of(
                        "map.nested-list.one[0].value", "one",
                        "map.nested-list.two[0].value", "two",
                        "map.nested-list.dashed-key[0].value", "dashed-value",
                        "map.nested-list.\"dotted.key\"[0].value", "dotted.value")) {
                    @Override
                    public Set<String> getPropertyNames() {
                        // indexed properties still need to query property names, because indexes are also dynamic
                        return super.getPropertyNames();
                    }
                })
                .withMapping(WithMapKeys.class)
                .build();

        WithMapKeys mapping = config.getConfigMapping(WithMapKeys.class);

        assertEquals("one", mapping.leaf().get("one"));
        assertEquals("two", mapping.leaf().get("two"));
        assertEquals("dashed-value", mapping.leaf().get("dashed-key"));
        assertEquals("dotted.value", mapping.leaf().get("dotted.key"));

        assertEquals("one", mapping.nested().get("one").value());
        assertEquals("two", mapping.nested().get("two").value());
        assertEquals("dashed-value", mapping.nested().get("dashed-key").value());
        assertEquals("dotted.value", mapping.nested().get("dotted.key").value());

        assertEquals("one", mapping.leafList().get("one").get(0));
        assertEquals("two", mapping.leafList().get("two").get(0));
        assertEquals("dashed-value", mapping.leafList().get("dashed-key").get(0));
        assertEquals("dotted.value", mapping.leafList().get("dotted.key").get(0));

        assertEquals("one", mapping.nestedList().get("one").get(0).value());
        assertEquals("two", mapping.nestedList().get("two").get(0).value());
        assertEquals("dashed-value", mapping.nestedList().get("dashed-key").get(0).value());
        assertEquals("dotted.value", mapping.nestedList().get("dotted.key").get(0).value());

        // TODO - Implement remaining pieces
        // - docs
    }

    @ConfigMapping(prefix = "map")
    interface WithMapKeys {
        @WithKeys(KeysProvider.class)
        Map<String, String> leaf();

        @WithKeys(KeysProvider.class)
        Map<String, Nested> nested();

        @WithKeys(KeysProvider.class)
        Map<String, List<String>> leafList();

        @WithKeys(KeysProvider.class)
        Map<String, List<Nested>> nestedList();

        interface Nested {
            String value();
        }

        class KeysProvider implements Supplier<Iterable<String>> {
            @Override
            public Iterable<String> get() {
                return List.of("one", "two", "dashed-key", "dotted.key");
            }
        }
    }

    @Test
    void requiredKeys() {
        assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withDefaultValue("required.nested.required.value", "required")
                .withMapping(EmptyKey.class).build());

        assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withDefaultValue("required.leaf.required", "required")
                .withMapping(EmptyKey.class).build());

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("required.nested.required.value", "required")
                .withDefaultValue("required.leaf.required", "required")
                .withMapping(RequiredKeys.class)
                .build();

        RequiredKeys mapping = config.getConfigMapping(RequiredKeys.class);
        assertEquals("required", mapping.leaf().get("required"));
        assertEquals("required", mapping.nested().get("required").value());
    }

    @ConfigMapping(prefix = "required")
    interface RequiredKeys {
        @WithKeys(RequiredKeysProvider.class)
        Map<String, String> leaf();

        @WithKeys(RequiredKeysProvider.class)
        Map<String, Nested> nested();

        interface Nested {
            String value();
        }

        class RequiredKeysProvider implements Supplier<Iterable<String>> {
            @Override
            public Iterable<String> get() {
                return List.of("required");
            }
        }
    }

    @Test
    void emptyKey() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("empty.nested.value", "value")
                .withMapping(EmptyKey.class)
                .build();

        EmptyKey mapping = config.getConfigMapping(EmptyKey.class);
        assertEquals("value", mapping.nested().get(null).value());
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyKey {
        @WithKeys(EmptyKeyProdiver.class)
        Map<String, Nested> nested();

        interface Nested {
            String value();
        }

        class EmptyKeyProdiver implements Supplier<Iterable<String>> {
            @Override
            public Iterable<String> get() {
                return List.of("");
            }
        }
    }

    @Test
    void emptyKeyWithUnnamedEmpty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("empty.nested.value", "value")
                .withMapping(EmptyKeyWithUnnamedEmpty.class)
                .build();

        EmptyKeyWithUnnamedEmpty mapping = config.getConfigMapping(EmptyKeyWithUnnamedEmpty.class);
        assertEquals("value", mapping.nested().get(null).value());
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyKeyWithUnnamedEmpty {
        @WithUnnamedKey
        @WithKeys(EmptyKeyProdiver.class)
        Map<String, Nested> nested();

        interface Nested {
            String value();
        }

        class EmptyKeyProdiver implements Supplier<Iterable<String>> {
            @Override
            public Iterable<String> get() {
                return List.of("");
            }
        }
    }

    @Test
    void emptyKeyWithUnnamedDefault() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("empty.nested.value", "value")
                .withMapping(EmptyKeyWithUnnamedDefault.class)
                .build();

        EmptyKeyWithUnnamedDefault mapping = config.getConfigMapping(EmptyKeyWithUnnamedDefault.class);
        assertEquals("value", mapping.nested().get(null).value());
        assertEquals("value", mapping.nested().get("default").value());
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyKeyWithUnnamedDefault {
        @WithUnnamedKey("default")
        @WithKeys(EmptyKeyProdiver.class)
        Map<String, Nested> nested();

        interface Nested {
            String value();
        }

        class EmptyKeyProdiver implements Supplier<Iterable<String>> {
            @Override
            public Iterable<String> get() {
                return List.of("");
            }
        }
    }

    @Test
    void keysWithParentName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("", Map.of("parent.one.value", "one")) {
                    @Override
                    public Set<String> getPropertyNames() {
                        return Collections.emptySet();
                    }
                })
                .withMapping(KeysWithParentName.class)
                .build();

        KeysWithParentName mapping = config.getConfigMapping(KeysWithParentName.class);
        assertEquals("one", mapping.nested().get("one").value());
    }

    @ConfigMapping(prefix = "parent")
    interface KeysWithParentName {
        @WithParentName
        @WithKeys(KeysProvider.class)
        Map<String, Nested> nested();

        interface Nested {
            String value();
        }

        class KeysProvider implements Supplier<Iterable<String>> {
            @Override
            public Iterable<String> get() {
                return List.of("one");
            }
        }
    }

    @Test
    void additionalKeys() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("", Map.of(
                        "additional.leaf.one", "one", "additional.leaf.two", "two")) {
                })
                .withMapping(AdditionalKeys.class);

        // additional.leaf.two in  does not map to any root
        assertThrows(ConfigValidationException.class, builder::build);
    }

    @ConfigMapping(prefix = "additional")
    interface AdditionalKeys {
        @WithKeys(KeysProvider.class)
        Map<String, String> leaf();

        class KeysProvider implements Supplier<Iterable<String>> {
            @Override
            public Iterable<String> get() {
                return List.of("one");
            }
        }
    }
}
