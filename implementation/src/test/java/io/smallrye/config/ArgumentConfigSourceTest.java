package io.smallrye.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArgumentConfigSourceTest {

    @Test
    public void shouldParseOptionsWithEquals() {
        ArgumentConfigSource source = new ArgumentConfigSource(new String[] {
                "-f=yes",
                "--version=1.0",
                "--arg-name=foo"
        });
        assertEquals("yes", source.getValue("f"));
        assertEquals("1.0", source.getValue("version"));
        assertEquals("foo", source.getValue("arg-name"));
    }

    @Test
    public void shouldParseEmptyOptions() {
        ArgumentConfigSource source = new ArgumentConfigSource(new String[] {
                "-f",
                "--version",
                "--arg-name"
        });
        assertEquals(ArgumentConfigSource.TRUE, source.getValue("f"));
        assertEquals(ArgumentConfigSource.TRUE, source.getValue("version"));
        assertEquals(ArgumentConfigSource.TRUE, source.getValue("arg-name"));
    }

    @Test
    public void shouldParseSeparateOptions() {
        ArgumentConfigSource source = new ArgumentConfigSource(new String[] {
                "-f", "foo.txt",
                "--arg-name",
                "--arg", "foo"
        });
        assertEquals("foo.txt", source.getValue("f"));
        assertEquals(ArgumentConfigSource.TRUE, source.getValue("arg-name"));
        assertEquals("foo", source.getValue("arg"));
    }

    @Test
    public void shouldSupportMultipleValuesAsSeparateArguments() {
        ArgumentConfigSource source = new ArgumentConfigSource(new String[] {
                "--foo=1",
                "--foo=2",
                "--foo=3"
        });
        assertEquals("1,2,3", source.getValue("foo"));
    }
}
