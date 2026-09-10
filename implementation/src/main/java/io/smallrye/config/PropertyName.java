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
 * <li><code>foo."bar[0]"</code> does not match <code>foo.bar[0]</code></li>
 * <li><code>foo."bar[0]"</code> does not match <code>foo."bar[*]"</code></li>
 * </ul>
 * <p>
 * Due to the equality rules and hashing function, {@link PropertyName} is <code>NOT</code> suitable for use in
 * structures that require an even distribution of keys.
 */
public record PropertyName(String name) {

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
     * @return <code>true</code> if both arguments match the {@link PropertyName} semantics,
     *         <code>false</code>
     *         otherwise.
     */
    public static boolean equals(final String name, final String other) {
        return equals(name, 0, name.length(), other, 0, other.length());
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
    @SuppressWarnings("squid:S4973")
    public static boolean equals(
            final String name, final int offset, final int len,
            final String other, final int ooffset, final int olen) {

        //noinspection StringEquality
        if (name == other && offset == ooffset && len == olen) {
            return true;
        }
        if (len == olen && name.regionMatches(offset, other, ooffset, len)) {
            return true;
        }
        // fast path, names that share a long prefix are the common case, so if the last character is different we
        // know there is no match.
        if (len != 0 && olen != 0) {
            char last = name.charAt(offset + len - 1);
            char olast = other.charAt(ooffset + olen - 1);
            if (last != olast && isPlain(last) && isPlain(olast)) {
                return false;
            }
        }
        // a wildcard stands for any segment, empty ones included, so "map.*" matches "map.\"\"", but it does not
        // stand for a leading empty segment, so "*" matches neither "" nor ".foo"
        if (startsEmpty(name, offset, offset + len) != startsEmpty(other, ooffset, ooffset + olen)) {
            return false;
        }

        if (matches(name, offset, offset + len, other, ooffset, ooffset + olen)) {
            return true;
        }
        // matching is symmetric, except for the greedy trailing star, so the other direction is only required
        // when it is the other name that ends in one
        if (other.indexOf('*', ooffset) == -1) {
            return false;
        }
        int last = lastSegmentStart(other, ooffset, ooffset + olen);
        if (!isStar(other, last, nameEnd(other, last, ooffset + olen))) {
            return false;
        }
        return matches(other, ooffset, ooffset + olen, name, offset, offset + len);
    }

    /**
     * Checks if the character only stands for itself, so it neither ends the segment nor carries any meaning.
     */
    private static boolean isPlain(final char c) {
        return c != '.' && c != '"' && c != '*' && c != '[' && c != ']' && c != '\\';
    }

    /**
     * Checks if the first segment of the region is empty, which only a leading dot or a leading quote can make it.
     */
    private static boolean startsEmpty(final String name, final int start, final int end) {
        if (start == end) {
            return true;
        }
        char c = name.charAt(start);
        if (c == '.') {
            return true;
        }
        // only a run of quotes can be an empty segment, so a single one, or one that a name follows, cannot
        if (c != '"' || start + 1 == end || name.charAt(start + 1) != '"') {
            return false;
        }
        return isEmpty(name, start, segmentEnd(name, start, end));
    }

    /**
     * Matches the segments of <code>name</code> against the segments of <code>other</code>, in order. A star segment
     * in <code>name</code> stands for any non empty segment in <code>other</code>, and a trailing star segment is
     * greedy, so it stands for every remaining segment.
     */
    private static boolean matches(
            final String name, final int start, final int end,
            final String other, final int ostart, final int oend) {

        int s = start;
        int os = ostart;
        segments: for (;;) {
            for (int i = s, o = os; i < end && o < oend;) {
                char c = name.charAt(i);
                char d = other.charAt(o);
                if (c == '.' && d == '.') {
                    // everything before matched and both segments end here
                    s = i + 1;
                    os = o + 1;
                    continue segments;
                } else if (c == '"' && !isQuoteRequired(name, start, end, i)) {
                    // a quote that carries no meaning only shifts the name being compared
                    i++;
                } else if (d == '"' && !isQuoteRequired(other, ostart, oend, o)) {
                    o++;
                } else if (c == '"' && isPlain(d) || d == '"' && isPlain(c)) {
                    // the quote that remains is one the segment requires, so it is a plain character that only
                    // another quote matches, and a segment holding one is never a wildcard
                    return false;
                } else if (!isPlain(c) || !isPlain(d)) {
                    break;
                } else if (c != d) {
                    // nothing later in either segment can realign what already differs
                    return false;
                } else {
                    i++;
                    o++;
                }
            }

            int e = segmentEnd(name, s, end);
            int oe = segmentEnd(other, os, oend);

            // greedy map - a trailing star segment consumes every remaining segment
            if (e == end && oe != oend && isStar(name, s, nameEnd(name, s, e))) {
                int last = lastSegmentStart(other, os, oend);
                if (hasWildcard(other, ostart, oend) || hasIndex(other, os, last)) {
                    return false;
                }
                return segmentMatches(name, s, e, other, last, oend);
            }

            if (!segmentMatches(name, s, e, other, os, oe)) {
                return false;
            }

            if (e == end || oe == oend) {
                return e == end && oe == oend;
            }

            s = e + 1;
            os = oe + 1;
        }
    }

    /**
     * Matches a single segment. Quotes that the segment does not require carry no meaning, so <code>"bar"</code>
     * matches <code>bar</code> and <code>"*"</code> is still the wildcard <code>*</code>. Quotes that the segment
     * does require are part of the name, so <code>"bar.baz"</code> is a single opaque segment, in which the star and
     * the brackets are plain characters.
     */
    private static boolean segmentMatches(
            final String name, final int start, final int end,
            final String other, final int ostart, final int oend) {
        int index = indexStart(name, start, end);
        int oindex = indexStart(other, ostart, oend);
        if ((index == -1) != (oindex == -1)) {
            return false;
        }
        if (index != -1 && !indexMatches(name, index + 1, end - 1, other, oindex + 1, oend - 1)) {
            return false;
        }

        int e = index == -1 ? end : index;
        int oe = oindex == -1 ? oend : oindex;
        if (isStar(name, start, e) || isStar(other, ostart, oe)) {
            return true;
        }
        return unquotedEquals(name, start, e, other, ostart, oe);
    }

    /**
     * Matches the contents of two indexes, where the star matches any index.
     */
    private static boolean indexMatches(
            final String name, final int start, final int end,
            final String other, final int ostart, final int oend) {

        boolean star = isStar(name, start, end);
        boolean ostar = isStar(other, ostart, oend);
        if (star && ostar) {
            return true;
        } else if (star) {
            return isNumeric(other, ostart, oend - ostart);
        } else if (ostar) {
            return isNumeric(name, start, end - start);
        }
        return isNumericEquals(name, start, end - start, other, ostart, oend - ostart);
    }

    /**
     * The end position of the segment that starts in <code>start</code>, which is the next dot that no quote holds,
     * or the end of the region.
     */
    private static int segmentEnd(final String name, final int start, final int end) {
        boolean quoted = false;
        for (int i = start; i < end; i++) {
            char c = name.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == '.' && !quoted) {
                return i;
            }
        }
        return end;
    }

    private static int lastSegmentStart(final String name, final int start, final int end) {
        int last = start;
        for (int s = start; s < end;) {
            int e = segmentEnd(name, s, end);
            if (e == end) {
                break;
            }
            s = e + 1;
            last = s;
        }
        return last;
    }

    /**
     * The end position of the name part of a segment, which is the position of its index, if it has one.
     */
    private static int nameEnd(final String name, final int start, final int end) {
        int index = indexStart(name, start, end);
        return index == -1 ? end : index;
    }

    /**
     * The position of the opening bracket of the index that ends the segment, or <code>-1</code> if the segment has
     * no index. Brackets that a quoted segment holds are plain characters, so <code>"bar[0]"</code> has no index,
     * while <code>bar[0]</code> and <code>"bar"[0]</code> both have one.
     */
    private static int indexStart(final String name, final int start, final int end) {
        // an index always closes the segment, so a segment that does not end in a bracket cannot hold one
        if (end - start < 3 || name.charAt(end - 1) != ']') {
            return -1;
        }

        int quote = name.indexOf('"', start);
        if (quote == -1 || quote >= end) {
            // without a quote to turn them into plain characters, the last opening bracket starts the index
            int open = name.lastIndexOf('[', end - 2);
            return open >= start ? open : -1;
        }

        boolean quoted = false;
        int open = -1;
        int close = -1;
        for (int i = start; i < end; i++) {
            char c = name.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (!quoted && c == '[') {
                open = i;
            } else if (!quoted && c == ']') {
                close = i;
            }
        }
        return close == end - 1 && open != -1 && open < close ? open : -1;
    }

    /**
     * Checks if the region holds a star that is a wildcard, which a greedy star cannot stand for. A star that a
     * quoted segment requiring its quotes holds is a plain character, so it is not a wildcard.
     */
    private static boolean hasWildcard(final String name, final int start, final int end) {
        int star = name.indexOf('*', start);
        if (star == -1 || star >= end) {
            return false;
        }
        for (int s = start; s < end;) {
            int e = segmentEnd(name, s, end);
            if (segmentContainsWildcard(name, s, e)) {
                return true;
            }
            if (e == end) {
                return false;
            }
            s = e + 1;
        }
        return false;
    }

    private static boolean segmentContainsWildcard(final String name, final int start, final int end) {
        for (int i = start; i < end; i++) {
            char c = name.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                int close = closingQuote(name, i, end);
                if (!hasRequiredQuotes(name, i + 1, close)) {
                    for (int k = i + 1; k < close; k++) {
                        if (name.charAt(k) == '\\') {
                            k++;
                        } else if (name.charAt(k) == '*') {
                            return true;
                        }
                    }
                }
                i = close;
            } else if (c == '*') {
                return true;
            }
        }
        return false;
    }

    private static int closingQuote(final String name, final int quote, final int end) {
        for (int i = quote + 1; i < end; i++) {
            char c = name.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return end;
    }

    /**
     * Checks if a quoted run requires its quotes, which is the case when it holds a segment boundary.
     */
    private static boolean hasRequiredQuotes(final String name, final int start, final int end) {
        for (int i = start; i < end; i++) {
            char c = name.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '.' || c == '[' || c == ']') {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the region is a single star, once the quotes that carry no meaning are removed, so both
     * <code>*</code> and <code>"*"</code> are a wildcard, while <code>"a.*"</code> is a plain name.
     */
    private static boolean isStar(final String name, final int start, final int end) {
        boolean star = false;
        for (int i = start; i < end; i++) {
            char c = name.charAt(i);
            if (c == '"' && !isQuoteRequired(name, start, end, i)) {
                // do nothing
            } else if (c == '*' && !star) {
                star = true;
            } else {
                return false;
            }
        }
        return star;
    }

    /**
     * Checks if the region holds nothing but quotes that carry no meaning, so it is an empty segment, which a
     * wildcard does not match.
     */
    private static boolean isEmpty(final String name, final int start, final int end) {
        for (int i = start; i < end; i++) {
            char c = name.charAt(i);
            if (c == '\\') {
                return false;
            } else if (c != '"' || isQuoteRequired(name, start, end, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if any segment that starts before <code>end</code> holds an index. A greedy star cannot swallow such a
     * segment, because the index it holds has nothing to match it.
     */
    private static boolean hasIndex(final String name, final int start, final int end) {
        int bracket = name.indexOf('[', start);
        if (bracket == -1 || bracket >= end) {
            return false;
        }
        for (int s = start; s < end;) {
            int e = segmentEnd(name, s, end);
            if (indexStart(name, s, e) != -1) {
                return true;
            }
            s = e + 1;
        }
        return false;
    }

    /**
     * Checks if the character in the given position is a quote that the segment requires, which is the case when the
     * quoted run holds a segment boundary. Such quotes are part of the comparison, so <code>"bar]"</code> does not
     * match <code>bar]</code>. A quote that the segment does not require carries no meaning and is not part of the
     * comparison, so <code>"bar"</code> matches <code>bar</code>. A quote that no other quote closes is required as
     * well, because {@link #segmentEnd} lets such a run swallow every dot that follows it, and both have to agree on
     * where the segment ends.
     */
    private static boolean isQuoteRequired(final String name, final int start, final int end, final int quote) {
        // if another quote precedes this one before a segment boundary, this is an inner quote of the run
        for (int i = quote - 1; i >= start; i--) {
            char c = name.charAt(i);
            if (c == '"') {
                return false;
            } else if (c == '.' || c == '[' || c == ']') {
                break;
            }
        }
        // a quote that closes the run before any segment boundary makes it one the segment does not require
        for (int i = quote + 1; i < end; i++) {
            char c = name.charAt(i);
            if (c == '"') {
                return false;
            } else if (c == '.' || c == '[' || c == ']') {
                break;
            }
        }
        return true;
    }

    /**
     * Compares two segments, skipping the quotes that carry no meaning. An escaped character is compared as it is,
     * so the escaped quote in <code>"bar\"baz"</code> is part of the name and does not delimit anything.
     */
    private static boolean unquotedEquals(final String name, final int start, final int end, final String other,
            final int ostart, final int oend) {
        int i = start;
        int o = ostart;
        for (;;) {
            while (i < end && name.charAt(i) == '"' && !isQuoteRequired(name, start, end, i)) {
                i++;
            }
            while (o < oend && other.charAt(o) == '"' && !isQuoteRequired(other, ostart, oend, o)) {
                o++;
            }
            if (i == end || o == oend) {
                return i == end && o == oend;
            }
            char c = name.charAt(i++);
            if (c != other.charAt(o++)) {
                return false;
            }
            if (c == '\\') {
                // the escaped character is part of the name, even when it is a quote
                if (i == end || o == oend) {
                    return i == end && o == oend;
                }
                if (name.charAt(i++) != other.charAt(o++)) {
                    return false;
                }
            }
        }
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
        int h = 0;
        int length = name.length();
        boolean quotesOpen = false;
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
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
}
