package io.smallrye.config;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jakarta.annotation.Priority;

import io.smallrye.config._private.ConfigMessages;

@Priority(Priorities.LIBRARY + 100)
public class SecretKeysConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 7291982039729980590L;

    private final Set<PropertyName> secrets;

    public SecretKeysConfigSourceInterceptor(final Set<String> secrets) {
        this.secrets = secrets.stream().map(PropertyName::new).collect(toSet());
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (SecretKeys.isLocked() && secrets.contains(new PropertyName(name))) {
            throw ConfigMessages.msg.notAllowed(name);
        }
        return context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        if (SecretKeys.isLocked()) {
            Set<String> names = new HashSet<>();
            Iterator<String> namesIterator = context.iterateNames();
            while (namesIterator.hasNext()) {
                String name = namesIterator.next();
                if (!secrets.contains(new PropertyName(name))) {
                    names.add(name);
                }
            }
            return names.iterator();
        }
        return context.iterateNames();
    }
}
