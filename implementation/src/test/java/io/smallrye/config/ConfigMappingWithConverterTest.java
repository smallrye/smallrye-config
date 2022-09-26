package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.AbstractConverter;

public class ConfigMappingWithConverterTest {
    @ConfigMapping
    interface WrongConverterType {
        @WithConverter(IntegerConverter.class)
        String type();

        class IntegerConverter implements Converter<Integer> {
            @Override
            public Integer convert(final String value) throws IllegalArgumentException, NullPointerException {
                return 0;
            }
        }
    }

    @ConfigMapping
    interface WrongAbstractConverterType {
        @WithConverter(IntegerConverter.class)
        String type();

        class IntegerConverter extends AbstractConverter<Integer> {
            @Override
            public Integer convert(final String value) throws IllegalArgumentException, NullPointerException {
                return 0;
            }
        }
    }

    @ConfigMapping
    interface WrongSuperConverterType {
        @WithConverter(IntegerConverter.class)
        String type();

        class IntegerConverter extends SuperConverter {

        }

        class SuperConverter implements Converter<Integer> {
            @Override
            public Integer convert(final String value) throws IllegalArgumentException, NullPointerException {
                return 0;
            }
        }
    }

    @Test
    void wrongConverter() {
        assertThrows(IllegalArgumentException.class, () -> config(WrongConverterType.class));
        assertThrows(IllegalArgumentException.class, () -> config(WrongAbstractConverterType.class));
        assertThrows(IllegalArgumentException.class, () -> config(WrongSuperConverterType.class));
    }

    @ConfigMapping
    interface SuperConverterType {
        @WithConverter(SuperConverter.class)
        Number number();

        class SuperConverter implements Converter<Integer> {
            @Override
            public Integer convert(final String value) throws IllegalArgumentException, NullPointerException {
                return Integer.valueOf(value);
            }
        }
    }

    @Test
    void superConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(SuperConverterType.class)
                .withDefaultValue("number", "1")
                .build();

        SuperConverterType mapping = config.getConfigMapping(SuperConverterType.class);
        assertEquals(1, mapping.number());
    }

    static void config(final Class<?> mapping) {
        new SmallRyeConfigBuilder().withMapping(mapping).build();
    }
}
