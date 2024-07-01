package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiValueTest {

    SmallRyeConfig config;

    @BeforeEach
    void setUp() {
        config = SmallRyeConfigProviderResolver.instance().getBuilder()
                .withSources(new PropertiesConfigSource(Map.of("my.pets", "snake,dog,cat,cat"), "my properties"))
                .build()
                .unwrap(SmallRyeConfig.class);
    }

    @Test
    void getValuesAsList() {
        List<String> pets = config.getValues("my.pets", String.class, ArrayList::new);
        assertNotNull(pets);
        assertEquals(4, pets.size());
        assertEquals(Arrays.asList("snake", "dog", "cat", "cat"), pets);
    }

    @Test
    void getValuesAsSet() {
        Set<String> pets = config.getValues("my.pets", String.class, HashSet::new);
        assertNotNull(pets);
        assertEquals(3, pets.size());
        assertTrue(pets.contains("snake"));
        assertTrue(pets.contains("dog"));
        assertTrue(pets.contains("cat"));
    }

    @Test
    void getValuesAsSortedSet() {
        Set<String> pets = config.getValues("my.pets", String.class, s -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        assertNotNull(pets);
        assertEquals(3, pets.size());
        assertEquals(Arrays.asList("cat", "dog", "snake"), new ArrayList<>(pets));
    }
}
