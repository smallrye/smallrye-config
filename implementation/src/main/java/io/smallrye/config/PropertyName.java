package io.smallrye.config;

import static io.smallrye.config.common.utils.StringUtil.isNumeric;
import static io.smallrye.config.common.utils.StringUtil.isNumericEquals;

/**
 * A configuration name.
 * <p>
 * While a configuration name is represented as a <code>String</code>, the equality rules are different, due to the
 * use of star (<code>*</code>), to match a segment in the name. A segment is a part of the configuration name
 * separated by a dot (<code>.</code>). For example:
 * <ul>
 * <li><code>foo.bar</code> matches <code>foo.*</code></li>
 * <li><code>foo.bar.baz</code> matches <code>foo.*.baz</code></li>
 * <li><code>foo."bar.baz"</code> matches <code>foo.*</code></li>
 * <li><code>foo.bar[0]</code> matches <code>foo.bar[*]</code></li>
 * </ul>
 */
public class PropertyName {
    private final String name;
    private final int hashCode;

    public PropertyName(final String name) {
        this.name = name;
        this.hashCode = hashCode(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PropertyName that = (PropertyName) o;
        return equals(this.name, that.name);
    }

    /**
     * Compares both arguments using {@link PropertyName} equals semantics.
     *
     * @param name a String with a configuration name.
     * @param other a String with another configuration name.
     * @return <code>true</code> if both arguments match the {@link PropertyName} semantics, <code>false</code>
     *         otherwise.
     */
    public static boolean equals(final String name, final String other) {
        return equalsInternal(name, 0, name.length(), other, 0, other.length())
                || equalsInternal(other, 0, other.length(), name, 0, name.length());
    }

    /**
     * Compares both arguments using {@link PropertyName} equals semantics for the specified regions.
     *
     * @param name a String with a configuration name.
     * @param offset the starting offset of the subregion in the String name.
     * @param len the number of characters to compare in the String name.
     * @param other a String with another configuration name.
     * @param ooffset the starting offset of the subregion in the String other.
     * @param olen the number of characters to compare in the String other.
     * @return <code>true</code> if both arguments match the {@link PropertyName} semantics, <code>false</code>
     *         otherwise.
     */
    public static boolean equals(final String name, final int offset, final int len, final String other, final int ooffset,
            final int olen) {
        return equalsInternal(name, offset, len, other, ooffset, olen)
                || equalsInternal(other, ooffset, olen, name, offset, len);
    }

    @SuppressWarnings("squid:S4973")
    private static boolean equalsInternal(final String name, final int offset, final int len, final String other,
            final int ooffset, final int olen) {
        //noinspection StringEquality
        if (name == other) {
            return true;
        }

        if (name.equals("*") && (other.isEmpty() || other.equals("\"\""))) {
            return false;
        }

        char n;
        char o;

        int matchPosition = offset + len - 1;
        for (int i = ooffset + olen - 1; i >= ooffset; i--) {
            if (matchPosition == -1) {
                return false;
            }

            o = other.charAt(i);
            n = name.charAt(matchPosition);

            if (n == '*') {
                if (o == ']') {
                    return false;
                } else if (o == '"') {
                    int beginQuote = other.lastIndexOf('"', i - 1);
                    if (beginQuote != -1) {
                        i = beginQuote;
                    }
                } else {
                    int previousDot = other.lastIndexOf('.', i);
                    if (previousDot != -1) {
                        i = previousDot + 1;
                    } else {
                        i = 0;
                    }
                    // greedy map - match any ending segments if a last segment is a *
                    if (matchPosition + 1 == len) {
                        // try to match by removing the * and the last segment
                        if (equalsInternal(name, offset, matchPosition, other, ooffset, olen - (ooffset + olen - i))) {
                            return true;
                        }
                        // try to match by keeping the original * and removing the last segment
                        return equalsInternal(name, offset, len, other, ooffset, olen - (ooffset + olen - i) - 1);
                    }
                }
            } else if (o == ']' && n == ']') {
                int otherBeginIndexed = other.lastIndexOf('[', i);
                int nameBeginIndexed = name.lastIndexOf('[', matchPosition);
                if (otherBeginIndexed != -1 && nameBeginIndexed != -1) {
                    if (other.charAt(otherBeginIndexed + 1) == '*' && name.charAt(nameBeginIndexed + 1) == '*') {
                        i = i - 2;
                        matchPosition = matchPosition - 3;
                    } else if (name.charAt(nameBeginIndexed + 1) == '*'
                            && isNumeric(other, otherBeginIndexed + 1, i - otherBeginIndexed - 1)) {
                        i = otherBeginIndexed;
                        matchPosition = matchPosition - 3;
                        // greedy map - match any ending segments if a last segment is a *[*]
                        if (matchPosition + 4 == len) {
                            // try to match by removing the * and the last segment
                            if (equalsInternal(name, offset, matchPosition + 1,
                                    other, ooffset, olen - (ooffset + olen - i)))
                                return true;
                            // try to match by keeping the original * and removing the last segment plus the brackets
                            return equalsInternal(name, offset, nameBeginIndexed, other,
                                    ooffset, olen - (ooffset + olen - otherBeginIndexed));
                        }
                    } else if (isNumericEquals(name, nameBeginIndexed + 1, matchPosition - nameBeginIndexed - 1, other,
                            otherBeginIndexed + 1, i - otherBeginIndexed - 1)) {
                        i = otherBeginIndexed;
                        matchPosition = nameBeginIndexed - 1;
                    } else {
                        return false;
                    }
                    continue;
                }
            } else if (o != n) {
                return false;
            }
            matchPosition--;
        }
        return matchPosition < offset;
    }

    /**
     * Calculate the hash code for the configuration name.
     * <p>
     * Due to the equality rules, the hash function is not suitable for use in structures that require an even
     * distribution of keys.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    private static int hashCode(final String name) {
        int h = 0;
        int length = name.length();
        boolean quotesOpen = false;
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (quotesOpen) {
                if (c == '"') {
                    quotesOpen = false;
                }
                continue;
            } else if (c == '"') {
                quotesOpen = true;
                continue;
            } else if (c != '[' && c != ']') {
                continue;
            }
            h = 31 * h + c;
        }
        return h;
    }

    @Override
    public String toString() {
        return name;
    }

    public static PropertyName name(final String name) {
        return new PropertyName(name);
    }

    public static PropertyName unprofiled(final String name) {
        if (!name.isEmpty() && name.charAt(0) == '%') {
            int profilesEnd = name.indexOf('.', 1);
            return new PropertyName(profilesEnd == -1 ? name : name.substring(profilesEnd + 1));
        } else {
            return new PropertyName(name);
        }
    }
}
