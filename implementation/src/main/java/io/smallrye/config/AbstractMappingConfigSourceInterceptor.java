package io.smallrye.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractMappingConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -3181156290079915301L;

    private final Function<String, String> mapping;

    public AbstractMappingConfigSourceInterceptor(final Function<String, String> mapping) {
        this.mapping = mapping != null ? mapping : Function.identity();
    }

    public AbstractMappingConfigSourceInterceptor(final Map<String, String> mappings) {
        this(new Function<String, String>() {
            @Override
            public String apply(final String name) {
                return mappings.getOrDefault(name, name);
            }
        });
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            final String name = namesIterator.next();
            names.add(name);
            final String mappedName = mapping.apply(name);
            if (mappedName != null) {
                names.add(mappedName);
            }
        }
        return names.iterator();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        final Set<ConfigValue> values = new HashSet<>();
        final Iterator<ConfigValue> valuesIterator = context.iterateValues();
        while (valuesIterator.hasNext()) {
            final ConfigValue value = valuesIterator.next();
            values.add(value);
            final String mappedName = mapping.apply(value.getName());
            if (mappedName != null) {
                values.add(ConfigValue.builder()
                        .withName(mappedName)
                        .withValue(value.getValue())
                        .withRawValue(value.getRawValue())
                        .withProfile(value.getProfile())
                        .withConfigSourceName(value.getConfigSourceName())
                        .withConfigSourcePosition(value.getConfigSourcePosition())
                        .withConfigSourceOrdinal(value.getConfigSourceOrdinal())
                        .withLineNumber(value.getLineNumber())
                        .build());
            }
        }
        return values.iterator();
    }

    protected Function<String, String> getMapping() {
        return mapping;
    }
}
