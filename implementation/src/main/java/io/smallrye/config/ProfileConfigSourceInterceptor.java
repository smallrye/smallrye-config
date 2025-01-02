package io.smallrye.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;
import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newTrimmingConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

@Priority(Priorities.LIBRARY + 200)
public class ProfileConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -6305289277993917313L;

    private static final Converter<ArrayList<String>> PROFILES_CONVERTER = newCollectionConverter(
            newTrimmingConverter(STRING_CONVERTER), new ArrayListFactory());

    private final List<String> profiles;
    private final List<String> prefixProfiles;

    public ProfileConfigSourceInterceptor(final String profile) {
        this(profile != null ? convertProfile(profile) : new ArrayList<>());
    }

    public ProfileConfigSourceInterceptor(final List<String> profiles) {
        List<String> reverseProfiles = new ArrayList<>(profiles);
        Collections.reverse(reverseProfiles);
        this.profiles = reverseProfiles;
        this.prefixProfiles = new ArrayList<>();
        for (String profile : this.profiles) {
            this.prefixProfiles.add("%" + profile + ".");
        }
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (!profiles.isEmpty()) {
            String normalizeName = activeName(name, profiles);
            ConfigValue profileValue = getProfileValue(context, normalizeName);
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
        for (int i = 0; i < profiles.size(); i++) {
            String profile = profiles.get(i);
            ConfigValue profileValue = context.proceed(prefixProfiles.get(i).concat(normalizeName));
            if (profileValue != null) {
                return profileValue.withProfile(profile);
            }
        }

        return null;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        return new Iterator<>() {
            final Iterator<String> iterator = context.iterateNames();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                return activeName(iterator.next(), profiles);
            }
        };
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public static String activeName(final String name, final List<String> profiles) {
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
                        int end = toOffset + profile.length();
                        if (end >= name.length()) {
                            continue;
                        }
                        char expectedEnd = name.charAt(end);
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
        List<String> profiles = PROFILES_CONVERTER.convert(profile);
        return profiles != null ? profiles : Collections.emptyList();
    }

    private static class ArrayListFactory implements IntFunction<ArrayList<String>> {

        @Override
        public ArrayList<String> apply(int value) {
            return new ArrayList<String>(value);
        }
    }
}
