package io.smallrye.config;

import java.util.Iterator;
import java.util.Map;
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
        return new Iterator<>() {
            final Iterator<String> iterator = context.iterateNames();
            String mappedName = null;

            @Override
            public boolean hasNext() {
                return mappedName != null || iterator.hasNext();
            }

            @Override
            public String next() {
                if (mappedName != null) {
                    String mappedName = this.mappedName;
                    this.mappedName = null;
                    return mappedName;
                }
                String name = iterator.next();
                String mappedName = mapping.apply(name);
                if (!name.equals(mappedName)) {
                    this.mappedName = mappedName;
                }
                return name;
            }
        };
    }

    protected Function<String, String> getMapping() {
        return mapping;
    }
}
