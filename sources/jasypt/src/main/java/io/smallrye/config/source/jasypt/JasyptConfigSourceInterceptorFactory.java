package io.smallrye.config.source.jasypt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class JasyptConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ContextConfigSource(context))
                .withMapping(JasyptConfig.class)
                .build();

        JasyptConfig jasyptConfig = config.getConfigMapping(JasyptConfig.class);

        return new JasyptConfigSourceInterceptor(jasyptConfig);
    }

    @Override
    public OptionalInt getPriority() {
        // So it evaluates before ExpressionConfigSourceInterceptor
        return OptionalInt.of(Priorities.LIBRARY + 300 - 1);
    }

    private static class ContextConfigSource implements ConfigSource {
        private final ConfigSourceInterceptorContext context;

        public ContextConfigSource(final ConfigSourceInterceptorContext context) {
            this.context = context;
        }

        @Override
        public Set<String> getPropertyNames() {
            Set<String> names = new HashSet<>();
            Iterator<String> namesIterator = context.iterateNames();
            while (namesIterator.hasNext()) {
                names.add(namesIterator.next());
            }
            return names;
        }

        @Override
        public String getValue(final String propertyName) {
            ConfigValue value = context.proceed(propertyName);
            return value != null && value.getValue() != null ? value.getValue() : null;
        }

        @Override
        public String getName() {
            return ContextConfigSource.class.getName();
        }
    }
}
