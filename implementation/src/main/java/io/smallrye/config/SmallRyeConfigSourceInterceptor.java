package io.smallrye.config;

import static io.smallrye.config.ConfigValueConfigSourceWrapper.wrap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

class SmallRyeConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 5513331820671039755L;

    private final ConfigValueConfigSource configSource;

    private SmallRyeConfigSourceInterceptor(final ConfigSource configSource) {
        this(wrap(configSource));
    }

    private SmallRyeConfigSourceInterceptor(final ConfigValueConfigSource configSource) {
        this.configSource = configSource;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        final ConfigValue configValue = configSource.getConfigValue(name);
        return configValue != null ? configValue : context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            names.add(namesIterator.next());
        }
        names.addAll(configSource.getPropertyNames());
        return names.iterator();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        final Set<ConfigValue> values = new HashSet<>();
        final Iterator<ConfigValue> valuesIterator = context.iterateValues();
        while (valuesIterator.hasNext()) {
            values.add(valuesIterator.next());
        }
        values.addAll(configSource.getConfigValueProperties().values());
        return values.iterator();
    }

    ConfigSource getSource() {
        if (configSource instanceof ConfigValueConfigSourceWrapper) {
            return ((ConfigValueConfigSourceWrapper) configSource).unwrap();
        }

        return configSource;
    }

    static ConfigSourceInterceptor configSourceInterceptor(final ConfigSource configSource) {
        return new SmallRyeConfigSourceInterceptor(configSource);
    }
}
