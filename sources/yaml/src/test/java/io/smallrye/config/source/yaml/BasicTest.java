package io.smallrye.config.source.yaml;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Assert;
import org.junit.Test;

public class BasicTest {

    @Test
    public void testBasicKeyValue() {
        String yaml = "foo:\n"
                + "    bar:\n"
                + "         baz: something\n"
                + "         zap: something else \n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        Assert.assertEquals("something", src.getValue("foo.bar.baz"));
        Assert.assertEquals("something else", src.getValue("foo.bar.zap"));
    }

    @Test
    public void testNullKeyValue() {
        String yaml = "foo:\n"
                + "    ~: something\n";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);

        Assert.assertEquals("something", src.getValue("foo"));
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

        Assert.assertEquals("cat,dog,chicken", src.getValue("foo.bar"));
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

        Assert.assertEquals("cat\\,dog,mouse\\,rat,chicken\\,turkey", src.getValue("foo.bar"));
    }
}
