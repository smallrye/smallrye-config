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

    protected Function<String, String> getMapping() {
        return mapping;
    }
}
