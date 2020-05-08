package io.smallrye.config;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Priority;

@Priority(Priorities.LIBRARY + 800)
public class ProfileConfigSourceInterceptor implements ConfigSourceInterceptor {
    public static final String SMALLRYE_PROFILE = "smallrye.config.profile";

    private static final long serialVersionUID = -6305289277993917313L;
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
            final String normalizeName = normalizeName(name);
            final ConfigValue profileValue = context.proceed("%" + profile + "." + normalizeName);
            if (profileValue != null) {
                final ConfigValue originalValue = context.proceed(normalizeName);
                if (originalValue != null && CONFIG_SOURCE_COMPARATOR.compare(profileValue, originalValue) > 0) {
                    return originalValue;
                } else {
                    return profileValue.withName(normalizeName);
                }
            }
        }

        return context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        context.iterateNames().forEachRemaining(name -> names.add(normalizeName(name)));
        return names.iterator();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        final Set<ConfigValue> values = new HashSet<>();
        context.iterateValues().forEachRemaining(value -> values.add(value.withName(normalizeName(value.getName()))));
        return values.iterator();
    }

    private String normalizeName(final String name) {
        return name.startsWith("%" + profile + ".") ? name.substring(profile.length() + 2) : name;
    }
}
