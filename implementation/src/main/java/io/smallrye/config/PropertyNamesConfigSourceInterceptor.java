package io.smallrye.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This interceptor adds additional entries to {@link org.eclipse.microprofile.config.Config#getPropertyNames}.
 */
class PropertyNamesConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 5263983885197566053L;

    private final Set<String> properties = new HashSet<>();

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        return context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            names.add(namesIterator.next());
        }
        names.addAll(properties);
        return names.iterator();
    }

    void addProperties(final Set<String> properties) {
        this.properties.addAll(properties);
    }
}
