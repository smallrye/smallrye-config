package io.smallrye.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;
import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newTrimmingConverter;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 200)
public class ProfileConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -6305289277993917313L;
    private final String[] profiles;

    public ProfileConfigSourceInterceptor(final String profile) {
        this(profile != null ? convertProfile(profile) : new ArrayList<>());
    }

    public ProfileConfigSourceInterceptor(final List<String> profiles) {
        List<String> reverseProfiles = new ArrayList<>(profiles);
        Collections.reverse(reverseProfiles);
        this.profiles = reverseProfiles.toArray(new String[0]);
    }

    public ProfileConfigSourceInterceptor(final ConfigSourceInterceptorContext context) {
        this(getProfile(context));
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (profiles.length > 0) {
            final String normalizeName = normalizeName(name);
            final ConfigValue profileValue = getProfileValue(context, normalizeName);
            if (profileValue != null) {
                try {
                    final ConfigValue originalValue = context.proceed(normalizeName);
                    if (originalValue != null && CONFIG_SOURCE_COMPARATOR.compare(originalValue, profileValue) > 0) {
                        return originalValue;
                    }
                } catch (final NoSuchElementException e) {
                    // We couldn't find the main property so we fallback to the profile property because it exists.
                }
                return profileValue.withName(normalizeName);
            }
        }

        return context.proceed(name);
    }

    public ConfigValue getProfileValue(final ConfigSourceInterceptorContext context, final String normalizeName) {
        for (String profile : profiles) {
            final ConfigValue profileValue = context.proceed("%" + profile + "." + normalizeName);
            if (profileValue != null) {
                return profileValue.withProfile(profile);
            }
        }

        return null;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            names.add(normalizeName(namesIterator.next()));
        }
        return names.iterator();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        final Set<ConfigValue> values = new HashSet<>();
        final Iterator<ConfigValue> valuesIterator = context.iterateValues();
        while (valuesIterator.hasNext()) {
            final ConfigValue value = valuesIterator.next();
            values.add(value.withName(normalizeName(value.getName())));
        }
        return values.iterator();
    }

    public String[] getProfiles() {
        return profiles;
    }

    private String normalizeName(final String name) {
        for (String profile : profiles) {
            if (name.startsWith("%" + profile + ".")) {
                return name.substring(profile.length() + 2);
            }
        }

        return name;
    }

    public static List<String> convertProfile(final String profile) {
        List<String> profiles = newCollectionConverter(newTrimmingConverter(STRING_CONVERTER), ArrayList::new).convert(profile);
        return profiles != null ? profiles : Collections.emptyList();
    }

    private static List<String> getProfile(final ConfigSourceInterceptorContext context) {
        final List<String> profiles = new ArrayList<>();
        profiles.addAll(getProfiles(context, SMALLRYE_CONFIG_PROFILE_PARENT));
        profiles.addAll(getProfiles(context, SMALLRYE_CONFIG_PROFILE));
        return profiles;
    }

    private static List<String> getProfiles(final ConfigSourceInterceptorContext context, final String propertyName) {
        final List<String> profiles = new ArrayList<>();
        final ConfigValue profileValue = context.proceed(propertyName);
        if (profileValue != null) {
            final List<String> convertProfiles = convertProfile(profileValue.getValue());
            for (String profile : convertProfiles) {
                profiles.addAll(getProfiles(context, "%" + profile + "." + SMALLRYE_CONFIG_PROFILE_PARENT));
                profiles.add(profile);
            }
        }
        return profiles;
    }
}
