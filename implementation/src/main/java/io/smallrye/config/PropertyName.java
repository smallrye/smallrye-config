package io.smallrye.config;

import static io.smallrye.config.common.utils.StringUtil.isNumeric;

public class PropertyName {
    private final String name;

    public PropertyName(final String name) {
        this.name = name;
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
        return equals(this.name, that.name) || equals(that.name, this.name);
    }

    static boolean equals(final String name, final String other) {
        return equalsInternal(name, other) || equalsInternal(other, name);
    }

    @SuppressWarnings("squid:S4973")
    private static boolean equalsInternal(final String name, final String other) {
        //noinspection StringEquality
        if (name == other) {
            return true;
        }

        if (name.equals("*") && (other.isEmpty() || other.equals("\"\""))) {
            return false;
        }

        char n;
        char o;

        int matchPosition = name.length() - 1;
        for (int i = other.length() - 1; i >= 0; i--) {
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
                }
            } else if (n == ']' && o == ']') {
                if (name.length() >= 3 && other.length() >= 3
                        && name.charAt(matchPosition - 1) == '*' && name.charAt(matchPosition - 2) == '['
                        && other.charAt(i - 1) == '*' && other.charAt(i - 2) == '[') {
                    matchPosition = matchPosition - 2;
                    i = i - 1;
                    continue;
                } else {
                    int beginIndexed = other.lastIndexOf('[', i);
                    if (beginIndexed != -1) {
                        int range = i - beginIndexed - 1;
                        if (isNumeric(other, beginIndexed + range, i)) {
                            matchPosition = matchPosition - 3;
                            i = i - range - 1;
                            continue;
                        }
                    }
                }
                return false;
            } else if (o != n) {
                return false;
            }
            matchPosition--;
        }
        return matchPosition <= 0;
    }

    @Override
    public int hashCode() {
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
            } else if (c != '.' && c != '[' && c != ']') {
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
}
