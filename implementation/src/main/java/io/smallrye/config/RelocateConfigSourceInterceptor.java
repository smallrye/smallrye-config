package io.smallrye.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;

import java.io.Serial;
import java.util.Map;
import java.util.function.Function;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 300)
public class RelocateConfigSourceInterceptor extends AbstractMappingConfigSourceInterceptor {
    @Serial
    private static final long serialVersionUID = 3476637906383945843L;

    public RelocateConfigSourceInterceptor(final Function<String, String> mapping) {
        super(mapping);
    }

    public RelocateConfigSourceInterceptor(final Map<String, String> mappings) {
        super(mappings);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        String map = getMapping().apply(name);
        ConfigValue relocateValue = context.proceed(map);

        if (name.equals(map)) {
            return relocateValue;
        }

        ConfigValue configValue = context.proceed(name);
        // Check which one comes from a higher ordinal source
        if (relocateValue != null && configValue != null) {
            return CONFIG_SOURCE_COMPARATOR.compare(relocateValue, configValue) >= 0 ? relocateValue
                    : configValue.withName(map);
        } else {
            if (relocateValue != null) {
                return relocateValue;
            } else if (configValue != null) {
                return configValue.withName(map);
            }
            return null;
        }
    }
}
