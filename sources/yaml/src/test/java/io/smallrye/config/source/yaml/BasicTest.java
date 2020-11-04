package io.smallrye.config.source.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

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
        String yaml = "";
        ConfigSource src = new YamlConfigSource("Yaml", yaml);
        assertNotNull(src, "Should create config source for empty file correctly");
    }

    @Test
    void compound() {
        ConfigSource src = new YamlConfigSource("Yaml",
                "foo:\n" +
                        "  bar:\n" +
                        "    val: foobar");
        assertEquals("foobar", src.getValue("foo.bar.val"));

        ConfigSource compact = new YamlConfigSource("Yaml",
                "foo.bar:\n" +
                        "  val: foobar");
        assertEquals("foobar", compact.getValue("foo.bar.val"));
        assertEquals("foobar", compact.getValue("\"foo.bar\".val"));
    }
}
