package io.smallrye.config.mapper;

import static io.smallrye.config.mapper.ConfigMapping.*;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 *
 */
public class MapperTests {

    public interface Basic {
        String helloWorld();

        @WithDefault("this is the default")
        String helloWorldWithDefault();

        Integer port();
    }

    @Test
    public void testBasic() throws ConfigurationValidationException {
        Builder mb = builder();
        mb.addRoot("test", Basic.class);
        ConfigMapping mapping = mb.build();
        SmallRyeConfigBuilder cb = new SmallRyeConfigBuilder();
        mapping.registerDefaultValues(cb);
        SmallRyeConfig config = cb.build();
        try {
            mapping.mapConfiguration(config);
        } catch (ConfigurationValidationException e) {
            assertEquals(2, e.getProblemCount());
        }

        cb = new SmallRyeConfigBuilder();
        cb.withSources(configSource(singletonMap("test.hello-world", "here I am!")));
        cb.withSources(configSource(singletonMap("test.port", "8080")));
        mapping.registerDefaultValues(cb);
        config = cb.build();
        Result result = mapping.mapConfiguration(config);
        Basic basic = result.getConfigRoot("test", Basic.class);
        assertEquals("here I am!", basic.helloWorld());
        assertEquals("this is the default", basic.helloWorldWithDefault());
        assertEquals(8080, basic.port().intValue());
    }

    public interface SomeTypes {
        int intProp();

        boolean boolProp();

        float floatProp();

        double doubleProp();

        long longProp();

        char charProp();

        Integer boxedIntProp();

        List<Integer> intListProp();

        int[] intArrayProp();

        OptionalInt optionalIntProp();
    }

    @Test
    public void testTypes() throws ConfigurationValidationException {
        Builder mb = builder();
        mb.addRoot("test.level2", SomeTypes.class);
        ConfigMapping mapping = mb.build();
        SmallRyeConfigBuilder cb = new SmallRyeConfigBuilder();
        mapping.registerDefaultValues(cb);
        cb.withSources(configSource(
                singletonMap("test.level2.int-prop", "1234"),
                singletonMap("test.level2.bool-prop", "true"),
                singletonMap("test.level2.float-prop", "12.25"), // choose an exact number for sanity
                singletonMap("test.level2.double-prop", "555.5"),
                singletonMap("test.level2.long-prop", "4000111222333"),
                singletonMap("test.level2.char-prop", "X"),
                singletonMap("test.level2.boxed-int-prop", "4096"),
                singletonMap("test.level2.int-list-prop", "6,22,77"),
                singletonMap("test.level2.int-array-prop", "10,9,8,7,3,2,1,0"),
                singletonMap("test.level2.optional-int-prop", "555")));
        SmallRyeConfig config = cb.build();
        Result result = mapping.mapConfiguration(config);
        SomeTypes someTypes = result.getConfigRoot("test.level2", SomeTypes.class);
        assertEquals(1234, someTypes.intProp());
        assertTrue(someTypes.boolProp());
        assertEquals(12.25f, someTypes.floatProp(), 0f);
        assertEquals(555.5, someTypes.doubleProp(), 0.0);
        assertEquals(4000111222333L, someTypes.longProp());
        assertEquals('X', someTypes.charProp());
        assertEquals(Integer.valueOf(4096), someTypes.boxedIntProp());
        assertEquals(Arrays.asList(Integer.valueOf(6), Integer.valueOf(22), Integer.valueOf(77)), someTypes.intListProp());
        assertArrayEquals(new int[] { 10, 9, 8, 7, 3, 2, 1, 0 }, someTypes.intArrayProp());
        assertTrue(someTypes.optionalIntProp().isPresent());
        assertEquals(555, someTypes.optionalIntProp().getAsInt());
    }

    public interface WithSubGroups {
        Optional<Basic> optBasic1();

        Optional<Basic> optBasic2();

        Basic reqBasic();
    }

    @Test
    public void testWithSubGroups() throws ConfigurationValidationException {
        Builder mb = builder();
        mb.addRoot("test", WithSubGroups.class);
        ConfigMapping mapping = mb.build();
        SmallRyeConfigBuilder cb = new SmallRyeConfigBuilder();
        mapping.registerDefaultValues(cb);
        cb.withSources(configSource(
                singletonMap("test.req-basic.hello-world", "surprise"),
                singletonMap("test.req-basic.hello-world-with-default", "non-default"),
                singletonMap("test.opt-basic2.hello-world", "present!")));
        SmallRyeConfig config = cb.build();
        Result result = mapping.mapConfiguration(config);
        WithSubGroups root = result.getConfigRoot("test", WithSubGroups.class);
        assertEquals("surprise", root.reqBasic().helloWorld());
        assertEquals("non-default", root.reqBasic().helloWorldWithDefault());
        assertFalse(root.optBasic1().isPresent());
        assertTrue(root.optBasic2().isPresent());
        assertEquals("present!", root.optBasic2().get().helloWorld());
        assertEquals("this is the default", root.optBasic2().get().helloWorldWithDefault());
    }

    public interface WithMaps {
        Map<String, WithSubGroups> theMap();
    }

    @Test
    public void testWithMaps() throws ConfigurationValidationException {
        Builder mb = builder();
        mb.addRoot("test", WithMaps.class);
        ConfigMapping mapping = mb.build();
        SmallRyeConfigBuilder cb = new SmallRyeConfigBuilder();
        mapping.registerDefaultValues(cb);
        cb.withSources(configSource(
                singletonMap("test.the-map.foo.req-basic.hello-world", "surprise"),
                singletonMap("test.the-map.foo.req-basic.hello-world-with-default", "non-default"),
                singletonMap("test.the-map.foo.opt-basic2.hello-world", "present!")));
        SmallRyeConfig config = cb.build();
        Result result = mapping.mapConfiguration(config);
        WithMaps root = result.getConfigRoot("test", WithMaps.class);
        assertEquals("surprise", root.theMap().get("foo").reqBasic().helloWorld());
        assertEquals("non-default", root.theMap().get("foo").reqBasic().helloWorldWithDefault());
        assertFalse(root.theMap().get("foo").optBasic1().isPresent());
        assertTrue(root.theMap().get("foo").optBasic2().isPresent());
        assertEquals("present!", root.theMap().get("foo").optBasic2().get().helloWorld());
        assertEquals("this is the default", root.theMap().get("foo").optBasic2().get().helloWorldWithDefault());
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    @SafeVarargs
    public static ConfigSource configSource(Map<String, String>... maps) {
        int length = maps.length;
        Map<String, String> map = new HashMap<>(length);
        for (Map<String, String> subMap : maps) {
            map.putAll(subMap);
        }
        return new MapBackedConfigSource("Test config source", map) {
        };
    }
}
