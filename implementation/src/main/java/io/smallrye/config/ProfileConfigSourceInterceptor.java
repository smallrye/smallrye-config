package io.smallrye.config;

import java.util.Comparator;
import java.util.Optional;

import javax.annotation.Priority;

@Priority(600)
public class ProfileConfigSourceInterceptor implements ConfigSourceInterceptor {
    public static final String SMALLRYE_PROFILE = "smallrye.config.profile";

    private static final Comparator<ConfigValue> CONFIG_SOURCE_COMPARATOR = (o1, o2) -> {
        int res = Integer.compare(o2.getConfigSourceOrdinal(), o1.getConfigSourceOrdinal());
        if (res != 0) {
            return res;
        }

        if (o1.getConfigSourceName() != null && o2.getConfigSourceName() != null) {
            return o2.getConfigSourceName().compareTo(o1.getConfigSourceName());
        } else {
            return res;
        }
    };

    private final String profile;

    public ProfileConfigSourceInterceptor(final String profile) {
        this.profile = profile;
    }

    public ProfileConfigSourceInterceptor(final ConfigSourceInterceptorContext context) {
        this(context, SMALLRYE_PROFILE);
    }

    public ProfileConfigSourceInterceptor(
            final ConfigSourceInterceptorContext context,
            final String profileConfigName) {
        this.profile = Optional.ofNullable(context.proceed(profileConfigName)).map(ConfigValue::getValue).orElse(null);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (profile != null) {
            final ConfigValue profileValue = context.proceed("%" + profile + "." + name);
            if (profileValue != null) {
                final ConfigValue originalValue = context.proceed(name);
                if (originalValue != null && CONFIG_SOURCE_COMPARATOR.compare(profileValue, originalValue) > 0) {
                    return originalValue;
                } else {
                    return profileValue;
                }
            }
        }

        return context.proceed(name);
    }
}
