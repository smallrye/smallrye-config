package io.smallrye.config.source.yaml.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.source.yaml.YamlConfigSource;

class BasicTest {
    @Test
    void basicKeyValue() {
        String yaml = "foo:\n"
                + "    bar:\n"
                + "         baz: something\n"
                + "         zap: something else \n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        assertEquals("something", src.getValue("foo.bar.baz"));
        assertEquals("something else", src.getValue("foo.bar.zap"));
    }

    @Test
    void nullKeyValue() {
        String yaml = "foo:\n"
                + "    ~: something\n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        assertEquals("something", src.getValue("foo"));
    }

    @Test
    void listValue() {
        String yaml = "foo:\n"
                + "     bar:\n"
                + "        ~:\n"
                + "           - cat\n"
                + "           - dog\n"
                + "           - chicken\n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        assertEquals("cat,dog,chicken", src.getValue("foo.bar"));
    }

    @Test
    void emptyFile() {
        ConfigSource src = new YamlConfigSource("Yaml", "");
        assertNotNull(src, "Should create config source for empty file correctly");
    }

    @Test
    void preserveOriginal() {
        ConfigSource source = new YamlConfigSource("Yaml", "date: 2010-10-10");
        assertEquals("2010-10-10", source.getValue("date"));
    }
}
