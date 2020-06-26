package io.smallrye.config.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

public class SmallRyeConfigConvertersBuilderTest {
    @Test
    public void withConverter() {
        final ConfigConverters converters = new SmallRyeConfigConvertersBuilder().withConverter(new DummyConverter()).build();

        final Converter<String> converter = converters.getConverter(String.class);
        System.out.println("converter = " + converter);
        assertNotNull(converter);
        assertEquals("dummy", converter.convert(""));
    }

    @Priority(1000)
    private static class DummyConverter implements Converter<String> {
        @Override
        public String convert(final String value) {
            return "dummy";
        }
    }
}
