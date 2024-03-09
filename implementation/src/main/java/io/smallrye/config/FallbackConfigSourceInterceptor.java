package io.smallrye.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;

import java.util.Map;
import java.util.function.Function;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 600)
public class FallbackConfigSourceInterceptor extends AbstractMappingConfigSourceInterceptor {
    private static final long serialVersionUID = 1472367702046537565L;

    public FallbackConfigSourceInterceptor(final Function<String, String> mapping) {
        super(mapping);
    }

    public FallbackConfigSourceInterceptor(final Map<String, String> mappings) {
        super(mappings);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);
        String map = getMapping().apply(name);

        if (name.equals(map)) {
            return configValue;
        }

        ConfigValue fallbackValue = context.proceed(map);
        // Check which one comes from a higher ordinal source
        if (configValue != null && fallbackValue != null) {
            return CONFIG_SOURCE_COMPARATOR.compare(configValue, fallbackValue) >= 0 ? configValue
                    : fallbackValue.withName(name);
        } else {
            if (configValue != null) {
                return configValue;
            } else if (fallbackValue != null) {
                return fallbackValue.withName(name);
            }
            return null;
        }
    }
}
