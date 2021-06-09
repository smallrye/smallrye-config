package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfigMappingProviderTest {
    @Test
    void skewer() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingProvider.skewer(""));
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingProvider.skewer("", '.'));

        assertEquals("my-property", ConfigMappingProvider.skewer("myProperty"));
        assertEquals("my.property", ConfigMappingProvider.skewer("myProperty", '.'));

        assertEquals("a", ConfigMappingProvider.skewer("a"));
        assertEquals("a", ConfigMappingProvider.skewer("a", '.'));

        assertEquals("my-property-abc", ConfigMappingProvider.skewer("myPropertyABC"));
        assertEquals("my.property.abc", ConfigMappingProvider.skewer("myPropertyABC", '.'));

        assertEquals("my-property-abc-abc", ConfigMappingProvider.skewer("myPropertyABCabc"));
        assertEquals("my.property.abc.abc", ConfigMappingProvider.skewer("myPropertyABCabc", '.'));
    }
}
