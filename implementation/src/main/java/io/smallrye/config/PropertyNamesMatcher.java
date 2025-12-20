package io.smallrye.config;

import java.util.List;
import java.util.Map;

/**
 * A matcher of configuration names.
 * <p>
 * Due to the equality rules of {@link PropertyName}, it is unsuitable to be used as is in hash-based
 * searches. Instead, this matcher splits the names by prefixes, which act as buckets, and then does a linear search.
 * <p>
 * Also avoids having to merge the prefix and the name into a single {@link PropertyName}, if both representations are
 * already separate for the matching.
 */
public final class PropertyNamesMatcher {
    private final Map<String, List<PropertyName>> prefixesAndNames;
    private final int maxPrefixSegments;

    private PropertyNamesMatcher(Map<String, List<PropertyName>> prefixesAndNames) {
        this.prefixesAndNames = prefixesAndNames;
        int maxPrefixSegments = 0;
        for (String prefix : prefixesAndNames.keySet()) {
            int current = countSegments(prefix);
            if (current > maxPrefixSegments) {
                maxPrefixSegments = current;
            }
        }
        this.maxPrefixSegments = maxPrefixSegments;
    }

    /**
     * Match a name with any of the names contained in this matcher.
     *
     * @param name the String to be matched
     * @return {@code true} if matched or {@code false} otherwise.
     */
    public boolean matches(final String name) {
        String prefix = null;
        List<PropertyName> propertyNames = null;
        NameIterator iterator = new NameIterator(name);
        // start backwards to match the max number of prefix segments
        for (int i = 0; i < maxPrefixSegments; i++) {
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                break;
            }
        }
        while (iterator.hasPrevious()) {
            prefix = iterator.getAllPreviousSegments();
            propertyNames = prefixesAndNames.get(prefix);
            if (propertyNames != null) {
                break;
            }
            iterator.previous();
        }
        if (propertyNames == null) {
            return false;
        }

        return matches(prefix, name, propertyNames);
    }

    /**
     * Match a prefix and a name with any of the names contained in this matcher.
     *
     * @param prefix the String to be matched, only the prefix
     * @param name the String to be matched, only the name after the prefix
     * @return {@code true} if matched or {@code false} otherwise.
     */
    public boolean matches(final String prefix, final String name) {
        return matches(prefix, name, prefixesAndNames.get(prefix));
    }

    private static boolean matches(final String prefix, final String name, final List<PropertyName> names) {
        for (PropertyName propertyName : names) {
            boolean matches = PropertyName.equals(
                    name, prefix.length() + 1, name.length() - prefix.length() - 1,
                    propertyName.getName(), 0, propertyName.getName().length());
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static int countSegments(final String prefix) {
        if (prefix.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < prefix.length(); i++) {
            if (prefix.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }

    public static PropertyNamesMatcher matcher(final Map<String, List<PropertyName>> prefixesAndNames) {
        return new PropertyNamesMatcher(prefixesAndNames);
    }
}
