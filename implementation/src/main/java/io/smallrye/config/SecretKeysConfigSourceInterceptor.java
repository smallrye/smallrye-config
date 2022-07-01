package io.smallrye.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Priority;

@Priority(Priorities.LIBRARY + 100)
public class SecretKeysConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 7291982039729980590L;

    private final Set<String> secrets;

    public SecretKeysConfigSourceInterceptor(final Set<String> secrets) {
        this.secrets = secrets;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (SecretKeys.isLocked() && isSecret(name)) {
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
                if (!secrets.contains(name)) {
                    names.add(name);
                }
            }
            return names.iterator();
        }
        return context.iterateNames();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        if (SecretKeys.isLocked()) {
            Set<ConfigValue> values = new HashSet<>();
            Iterator<ConfigValue> valuesIterator = context.iterateValues();
            while (valuesIterator.hasNext()) {
                ConfigValue value = valuesIterator.next();
                if (!secrets.contains(value.getName())) {
                    values.add(value);
                }
            }
            return values.iterator();
        }
        return context.iterateValues();
    }

    private boolean isSecret(final String name) {
        return secrets.contains(name);
    }
}
