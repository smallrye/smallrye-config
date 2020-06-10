package io.smallrye.config.source.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

public class BasicTest {

    @Test
    public void testBasicKeyValue() {
        String yaml = "foo:\n"
                + "    bar:\n"
                + "         baz: something\n"
                + "         zap: something else \n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        assertEquals("something", src.getValue("foo.bar.baz"));
        assertEquals("something else", src.getValue("foo.bar.zap"));
    }

    @Test
    public void testNullKeyValue() {
        String yaml = "foo:\n"
                + "    ~: something\n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        assertEquals("something", src.getValue("foo"));
    }

    @Test
    public void testListValue() {
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
    public void testListOfListValue() {
        String yaml = "foo:\n"
                + "     bar:\n"
                + "        ~:\n"
                + "           - [cat, dog]\n"
                + "           - [mouse, rat]\n"
                + "           - [chicken, turkey]\n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        assertEquals("cat\\,dog,mouse\\,rat,chicken\\,turkey", src.getValue("foo.bar"));
    }

    @Test
    public void testEmptyFile() {
        String yaml = "";
        ConfigSource src = new YamlConfigSource("Yaml", yaml);
        assertNotNull(src, "Should create config source for empty file correctly");
    }
}
