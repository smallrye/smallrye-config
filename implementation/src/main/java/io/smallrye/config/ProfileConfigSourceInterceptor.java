package io.smallrye.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;
import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newTrimmingConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (profiles.length > 0) {
            final String normalizeName = activeName(name, profiles);
            final ConfigValue profileValue = getProfileValue(context, normalizeName);
            if (profileValue != null) {
                final ConfigValue originalValue = context.proceed(normalizeName);
                if (originalValue != null && CONFIG_SOURCE_COMPARATOR.compare(originalValue, profileValue) > 0) {
                    return originalValue;
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
            names.add(activeName(namesIterator.next(), profiles));
        }
        return names.iterator();
    }

    public String[] getProfiles() {
        return profiles;
    }

    public static String activeName(final String name, final String[] profiles) {
        if (!name.isEmpty() && name.charAt(0) == '%') {
            int profilesEnd = name.indexOf('.', 1);
            int multipleSplit = -1;
            for (int i = 1; i < profilesEnd; i++) {
                if (name.charAt(i) == ',') {
                    multipleSplit = i;
                    break;
                }
            }

            if (multipleSplit == -1) {
                // Single profile property name (%profile.foo.bar)
                for (String profile : profiles) {
                    if (profilesEnd == profile.length() + 1 && name.regionMatches(1, profile, 0, profile.length())) {
                        return name.substring(profilesEnd + 1);
                    }
                }
            } else {
                // Multiple profile property name (%profile,another.foo.bar)
                int nextSplit = multipleSplit;
                int toOffset = 1;
                while (nextSplit != -1) {
                    for (String profile : profiles) {
                        char expectedEnd = name.charAt(toOffset + profile.length());
                        if ((expectedEnd == '.' || expectedEnd == ',') &&
                                name.regionMatches(toOffset, profile, 0, profile.length())) {
                            return name.substring(profilesEnd + 1);
                        }
                    }

                    toOffset = nextSplit + 1;
                    nextSplit = -1;

                    for (int i = toOffset; i < profilesEnd; i++) {
                        if (name.charAt(i) == ',') {
                            nextSplit = i;
                            break;
                        }
                    }

                    if (toOffset < profilesEnd && nextSplit == -1) {
                        nextSplit = profilesEnd;
                    }
                }
            }
        }
        return name;
    }

    public static List<String> convertProfile(final String profile) {
        List<String> profiles = newCollectionConverter(newTrimmingConverter(STRING_CONVERTER), ArrayList::new).convert(profile);
        return profiles != null ? profiles : Collections.emptyList();
    }
}
