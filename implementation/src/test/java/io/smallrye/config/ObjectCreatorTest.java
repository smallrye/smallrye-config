package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappingContext.MapWithDefault;

public class ObjectCreatorTest {
    @Test
    void objectCreator() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "unnamed.value", "unnamed",
                        "unnamed.key.value", "value"))
                .withSources(config(
                        "list-map[0].key", "value",
                        "list-map[0].another", "another"))
                .withSources(config(
                        "list-group[0].value", "value"))
                .withSources(config(
                        "map-group.key.value", "value"))
                .withSources(config(
                        "optional-group.value", "value"))
                .withSources(config(
                        "group.value", "value"))
                .withSources(config(
                        "value", "value"))
                .withSources(config(
                        "optional-value", "value"))
                .withSources(config(
                        "optional-list", "value"))
                .withSources(config(
                        "optional-list-group[0].value", "value"))
                .build();

        ConfigMappingContext context = new ConfigMappingContext(config, new HashMap<>());
        ObjectCreator mapping = new ObjectCreatorImpl(context);

        assertEquals(2, mapping.unnamed().size());
        assertEquals("unnamed", mapping.unnamed().get("unnamed").value());
        assertEquals("value", mapping.unnamed().get("key").value());

        assertEquals("value", mapping.listMap().get(0).get("key"));
        assertEquals("another", mapping.listMap().get(0).get("another"));

        assertEquals("value", mapping.listGroup().get(0).value());

        assertEquals("value", mapping.mapGroup().get("key").value());

        assertTrue(mapping.optionalGroup().isPresent());
        assertEquals("value", mapping.optionalGroup().get().value());
        assertTrue(mapping.optionalGroupMissing().isEmpty());
        assertEquals("value", mapping.group().value());

        assertEquals("value", mapping.value());
        assertTrue(mapping.optionalValue().isPresent());
        assertEquals("value", mapping.optionalValue().get());
        assertTrue(mapping.optionalList().isPresent());
        assertEquals("value", mapping.optionalList().get().get(0));
        assertTrue(mapping.optionalListGroup().isPresent());
        assertEquals("value", mapping.optionalListGroup().get().get(0).value());
        assertTrue(mapping.optionalListGroupMissing().isEmpty());

        assertTrue(context.getProblems().isEmpty());
    }

    @ConfigMapping
    interface ObjectCreator {
        @WithUnnamedKey
        Map<String, Nested> unnamed();

        List<Map<String, String>> listMap();

        List<Nested> listGroup();

        Map<String, Nested> mapGroup();

        Optional<Nested> optionalGroup();

        Optional<Nested> optionalGroupMissing();

        Nested group();

        String value();

        Optional<String> optionalValue();

        Optional<List<String>> optionalList();

        Optional<List<Nested>> optionalListGroup();

        Optional<List<Nested>> optionalListGroupMissing();

        interface Nested {
            String value();
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static class ObjectCreatorImpl implements ObjectCreator {
        Map<String, Nested> unnamed;
        List<Map<String, String>> listMap;
        List<Nested> listGroup;
        Map<String, Nested> mapGroup;
        Optional<Nested> optionalGroup;
        Optional<Nested> optionalGroupMissing;
        Nested group;
        String value;
        Optional<String> optionalValue;
        Optional<List<String>> optionalList;
        Optional<List<Nested>> optionalListGroup;
        Optional<List<Nested>> optionalListGroupMissing;

        @SuppressWarnings("unchecked")
        public ObjectCreatorImpl(ConfigMappingContext context) {
            StringBuilder sb = context.getNameBuilder();
            int length = sb.length();
            ConfigMapping.NamingStrategy ns = ConfigMapping.NamingStrategy.KEBAB_CASE;

            sb.append(ns.apply("unnamed"));
            ConfigMappingContext.ObjectCreator<Map<String, Nested>> unnamed = context.new ObjectCreator<Map<String, Nested>>(
                    sb.toString())
                    .map(String.class, null, "unnamed", null)
                    .lazyGroup(Nested.class);
            this.unnamed = unnamed.get();
            sb.setLength(length);

            sb.append(ns.apply("list-map"));
            ConfigMappingContext.ObjectCreator<List<Map<String, String>>> listMap = context.new ObjectCreator<List<Map<String, String>>>(
                    sb.toString()).collection(List.class).values(String.class, null, String.class, null, emptyList(), null);
            this.listMap = listMap.get();
            sb.setLength(length);

            sb.append(ns.apply("list-group"));
            ConfigMappingContext.ObjectCreator<List<Nested>> listGroup = context.new ObjectCreator<List<Nested>>(sb.toString())
                    .collection(List.class).group(Nested.class);
            this.listGroup = listGroup.get();
            sb.setLength(length);

            sb.append(ns.apply("map-group"));
            ConfigMappingContext.ObjectCreator<Map<String, Nested>> mapGroup = context.new ObjectCreator<Map<String, Nested>>(
                    sb.toString()).map(String.class, null).lazyGroup(Nested.class);
            this.mapGroup = mapGroup.get();
            sb.setLength(length);

            sb.append(ns.apply("optional-group"));
            ConfigMappingContext.ObjectCreator<Optional<Nested>> optionalGroup = context.new ObjectCreator<Optional<Nested>>(
                    sb.toString()).optionalGroup(Nested.class);
            this.optionalGroup = optionalGroup.get();
            sb.setLength(length);

            sb.append(ns.apply("optional-group-missing"));
            ConfigMappingContext.ObjectCreator<Optional<Nested>> optionalGroupMissing = context.new ObjectCreator<Optional<Nested>>(
                    sb.toString()).optionalGroup(Nested.class);
            this.optionalGroupMissing = optionalGroupMissing.get();
            sb.setLength(length);

            sb.append(ns.apply("group"));
            ConfigMappingContext.ObjectCreator<Nested> group = context.new ObjectCreator<Nested>(sb.toString())
                    .group(Nested.class);
            this.group = group.get();
            sb.setLength(length);

            sb.append(ns.apply("value"));
            ConfigMappingContext.ObjectCreator<String> value = context.new ObjectCreator<String>(sb.toString())
                    .value(String.class, null);
            this.value = value.get();
            sb.setLength(length);

            sb.append(ns.apply("optional-value"));
            ConfigMappingContext.ObjectCreator<Optional<String>> optionalValue = context.new ObjectCreator<Optional<String>>(
                    sb.toString()).optionalValue(String.class, null);
            this.optionalValue = optionalValue.get();
            sb.setLength(length);

            sb.append(ns.apply("optional-value"));
            ConfigMappingContext.ObjectCreator<Optional<List<String>>> optionalList = context.new ObjectCreator<Optional<List<String>>>(
                    sb.toString()).optionalValues(String.class, null, List.class);
            this.optionalList = optionalList.get();
            sb.setLength(length);

            sb.append(ns.apply("optional-list-group"));
            ConfigMappingContext.ObjectCreator<Optional<List<Nested>>> optionalListGroup = context.new ObjectCreator<Optional<List<Nested>>>(
                    sb.toString()).optionalCollection(List.class).group(Nested.class);
            this.optionalListGroup = optionalListGroup.get();
            sb.setLength(length);

            sb.append(ns.apply("optional-list-group-missing"));
            ConfigMappingContext.ObjectCreator<Optional<List<Nested>>> optionalListGroupMissing = context.new ObjectCreator<Optional<List<Nested>>>(
                    sb.toString()).optionalCollection(List.class).group(Nested.class);
            this.optionalListGroupMissing = optionalListGroupMissing.get();
            sb.setLength(length);
        }

        @Override
        public Map<String, Nested> unnamed() {
            return unnamed;
        }

        @Override
        public List<Map<String, String>> listMap() {
            return listMap;
        }

        @Override
        public List<Nested> listGroup() {
            return listGroup;
        }

        @Override
        public Map<String, Nested> mapGroup() {
            return mapGroup;
        }

        @Override
        public Optional<Nested> optionalGroup() {
            return optionalGroup;
        }

        @Override
        public Optional<Nested> optionalGroupMissing() {
            return optionalGroupMissing;
        }

        @Override
        public Nested group() {
            return group;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public Optional<String> optionalValue() {
            return optionalValue;
        }

        @Override
        public Optional<List<String>> optionalList() {
            return optionalList;
        }

        @Override
        public Optional<List<Nested>> optionalListGroup() {
            return optionalListGroup;
        }

        @Override
        public Optional<List<Nested>> optionalListGroupMissing() {
            return optionalListGroupMissing;
        }
    }

    @Test
    void optionalGroup() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "optional.value", "value"))
                .build();

        ConfigMappingContext context = new ConfigMappingContext(config, new HashMap<>());
        OptionalGroup mapping = new OptionalGroupImpl(context);

        assertTrue(mapping.optional().isPresent());
        assertTrue(mapping.empty().isEmpty());

        assertTrue(context.getProblems().isEmpty());
    }

    @ConfigMapping
    interface OptionalGroup {
        Optional<Nested> optional();

        Optional<Nested> empty();

        interface Nested {
            String value();
        }
    }

    static class OptionalGroupImpl implements OptionalGroup {
        Optional<Nested> optional;
        Optional<Nested> empty;

        public OptionalGroupImpl(ConfigMappingContext context) {
            StringBuilder sb = context.getNameBuilder();
            int length = sb.length();
            ConfigMapping.NamingStrategy ns = ConfigMapping.NamingStrategy.KEBAB_CASE;

            sb.append(ns.apply("optional"));
            ConfigMappingContext.ObjectCreator<Optional<Nested>> optional = context.new ObjectCreator<Optional<Nested>>(
                    sb.toString()).optionalGroup(Nested.class);
            this.optional = optional.get();
            sb.setLength(length);

            sb.append(ns.apply("empty"));
            ConfigMappingContext.ObjectCreator<Optional<Nested>> empty = context.new ObjectCreator<Optional<Nested>>(
                    sb.toString()).optionalGroup(Nested.class);
            this.empty = empty.get();
            sb.setLength(length);
        }

        @Override
        public Optional<Nested> optional() {
            return optional;
        }

        @Override
        public Optional<Nested> empty() {
            return empty;
        }
    }

    @Test
    void unnamedKeys() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(UnnamedKeys.class)
                .withSources(config(
                        "unnamed.value", "unnamed",
                        "unnamed.key.value", "value"))
                .build();

        ConfigMappingContext context = new ConfigMappingContext(config, new HashMap<>());
        context.applyRootPath("unnamed");

        UnnamedKeys mapping = new UnnamedKeysImpl(context);
        assertEquals("unnamed", mapping.map().get(null).value());
        assertEquals("value", mapping.map().get("key").value());

        mapping = config.getConfigMapping(UnnamedKeys.class);
        assertEquals("unnamed", mapping.map().get(null).value());
        assertEquals("value", mapping.map().get("key").value());

        assertTrue(context.getProblems().isEmpty());
    }

    @ConfigMapping(prefix = "unnamed")
    interface UnnamedKeys {
        @WithUnnamedKey
        @WithParentName
        Map<String, Nested> map();

        interface Nested {
            String value();
        }
    }

    static class UnnamedKeysImpl implements UnnamedKeys {
        Map<String, Nested> map;

        public UnnamedKeysImpl(ConfigMappingContext context) {
            StringBuilder sb = context.getNameBuilder();
            int length = sb.length();

            ConfigMappingContext.ObjectCreator<Map<String, Nested>> map = context.new ObjectCreator<Map<String, Nested>>(
                    sb.toString())
                    .map(String.class, null, "", null)
                    .lazyGroup(Nested.class);
            this.map = map.get();
            sb.setLength(length);
        }

        @Override
        public Map<String, Nested> map() {
            return map;
        }
    }

    @Test
    void mapDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapDefaults.class)
                .withSources(config(
                        "map.defaults.one", "value"))
                .withSources(config(
                        "map.defaults-nested.one.value", "value"))
                .withSources(config(
                        "map.defaults-list.one[0].value", "value"))
                .build();

        ConfigMappingContext context = new ConfigMappingContext(config, new HashMap<>());
        context.applyRootPath("map");
        MapDefaults mapping = new MapDefaultsImpl(context);

        assertEquals("value", mapping.defaults().get("one"));
        assertEquals("default", mapping.defaults().get("default"));
        assertEquals("default", mapping.defaults().get("something"));

        assertEquals("value", mapping.defaultsNested().get("one").value());
        assertEquals("default", mapping.defaultsNested().get("default").value());
        assertEquals("another", mapping.defaultsNested().get("default").another().another());

        assertEquals("value", mapping.defaultsList().get("one").get(0).value());

        assertTrue(context.getProblems().isEmpty());
    }

    @ConfigMapping(prefix = "map")
    interface MapDefaults {
        @WithDefault("default")
        Map<String, String> defaults();

        @WithDefaults
        Map<String, Nested> defaultsNested();

        @WithDefaults
        Map<String, List<Nested>> defaultsList();

        interface Nested {
            @WithDefault("default")
            String value();

            AnotherNested another();

            interface AnotherNested {
                @WithDefault("another")
                String another();
            }
        }
    }

    @SuppressWarnings("unchecked")
    static class MapDefaultsImpl implements MapDefaults {
        Map<String, String> defaults;
        Map<String, Nested> defaultsNested;
        Map<String, List<Nested>> defaultsList;

        public MapDefaultsImpl(ConfigMappingContext context) {
            StringBuilder sb = context.getNameBuilder();
            int length = sb.length();
            ConfigMapping.NamingStrategy ns = ConfigMapping.NamingStrategy.KEBAB_CASE;

            sb.append(".");
            sb.append(ns.apply("defaults"));
            ConfigMappingContext.ObjectCreator<Map<String, String>> defaults = context.new ObjectCreator<Map<String, String>>(
                    sb.toString())
                    .values(String.class, null, String.class, null, emptyList(), "default");
            this.defaults = defaults.get();
            sb.setLength(length);

            sb.append(".");
            sb.append(ns.apply("defaults-nested"));
            ConfigMappingContext.ObjectCreator<Map<String, Nested>> defaultsNested = context.new ObjectCreator<Map<String, Nested>>(
                    sb.toString())
                    .map(String.class, null, null, null, new Supplier<Nested>() {
                        @Override
                        public Nested get() {
                            sb.append(".*");
                            Nested nested = context.constructGroup(Nested.class);
                            sb.setLength(length);
                            return nested;
                        }
                    })
                    .lazyGroup(Nested.class);
            this.defaultsNested = defaultsNested.get();
            sb.setLength(length);

            sb.append(".");
            sb.append(ns.apply("defaults-list"));
            ConfigMappingContext.ObjectCreator<Map<String, List<Nested>>> defaultsList = context.new ObjectCreator<Map<String, List<Nested>>>(
                    sb.toString())
                    .map(String.class, null)
                    .collection(List.class)
                    .lazyGroup(Nested.class);
            this.defaultsList = defaultsList.get();
            sb.setLength(length);
        }

        @Override
        public Map<String, String> defaults() {
            return defaults;
        }

        @Override
        public Map<String, Nested> defaultsNested() {
            return defaultsNested;
        }

        @Override
        public Map<String, List<Nested>> defaultsList() {
            return defaultsList;
        }
    }

    @Test
    void namingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "naming.nested_value.value", "value"))
                .build();

        ConfigMappingContext context = new ConfigMappingContext(config, new HashMap<>());
        context.applyRootPath("naming");
        Naming naming = new NamingImpl(context);

        assertEquals("value", naming.nestedValue().value());

        assertTrue(context.getProblems().isEmpty());
    }

    @ConfigMapping(prefix = "naming", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface Naming {
        Nested nestedValue();

        interface Nested {
            String value();
        }
    }

    static class NamingImpl implements Naming {
        Nested nestedValue;

        public NamingImpl(ConfigMappingContext context) {
            ConfigMapping.NamingStrategy ns = ConfigMapping.NamingStrategy.SNAKE_CASE;
            StringBuilder sb = context.getNameBuilder();
            int length = sb.length();

            sb.append(".");
            sb.append(ns.apply("nestedValue"));
            ConfigMappingContext.ObjectCreator<Nested> nestedValue = context.new ObjectCreator<Nested>(sb.toString())
                    .group(Nested.class);
            this.nestedValue = nestedValue.get();
            sb.setLength(length);
        }

        @Override
        public Nested nestedValue() {
            return nestedValue;
        }
    }

    @Test
    void splitRootsRequiredGroup() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "false"))
                .withDefaultValue("nested.nested.something", "something")
                .withMapping(SplitRootsRequiredGroup.class)
                .build();

        SplitRootsRequiredGroup mapping = config.getConfigMapping(SplitRootsRequiredGroup.class);
        assertTrue(mapping.nested().isEmpty());
    }

    @ConfigMapping
    interface SplitRootsRequiredGroup {

        Optional<NestedOptional> nested();

        interface NestedOptional {
            @WithName("x")
            Optional<Nested> nestedOpt();
        }

        interface Nested {
            String value();
        }
    }

    @Test
    void hierarchy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "base.nested.base", "value",
                        "base.nested.value", "value"))
                .withMapping(ExtendsBase.class)
                .build();

        ExtendsBase mapping = config.getConfigMapping(ExtendsBase.class);

        assertTrue(mapping.nested().isPresent());
        assertEquals("value", mapping.nested().get().base());
        assertEquals("value", mapping.nested().get().value());
    }

    public interface Base {
        Optional<Nested> nested();

        interface NestedBase {
            String base();
        }

        interface Nested extends NestedBase {
            String value();
        }
    }

    @ConfigMapping(prefix = "base")
    public interface ExtendsBase extends Base {

    }

    @SuppressWarnings({
            "MismatchedQueryAndUpdateOfCollection",
            "RedundantOperationOnEmptyContainer"
    })
    @Test
    void mapWithDefault() {
        MapWithDefault<String, String> map = new MapWithDefault<>("default");
        assertTrue(map.isEmpty());
        assertFalse(map.entrySet().iterator().hasNext());

        assertEquals("default", map.get("default"));
        assertEquals("default", map.get("one"));
        assertEquals("default", map.get("two"));

        assertNull(map.getOrDefault("default", null));
    }
}
