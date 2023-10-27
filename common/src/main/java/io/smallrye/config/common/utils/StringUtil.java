/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.config.common.utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class StringUtil {

    private static final String[] NO_STRINGS = new String[0];

    private static final Pattern ITEM_PATTERN = Pattern.compile("(,+)|([^\\\\,]+)|\\\\(.)");

    private StringUtil() {
    }

    public static String[] split(String text) {
        if (text == null || text.isEmpty()) {
            return NO_STRINGS;
        }
        final Matcher matcher = ITEM_PATTERN.matcher(text);
        String item = null;
        StringBuilder b = null;
        ArrayList<String> list = new ArrayList<>(4);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // delimiter
                if (item != null) {
                    list.add(item);
                    item = null;
                }
            } else if (matcher.group(2) != null) {
                // regular text blob
                assert item == null : "Regular expression matching malfunctioned";
                item = matcher.group(2);
            } else if (matcher.group(3) != null) {
                // escaped text
                if (b == null) {
                    b = new StringBuilder();
                }
                if (item != null) {
                    b.append(item);
                    item = null;
                }
                b.append(matcher.group(3));
                while (matcher.find()) {
                    if (matcher.group(1) != null) {
                        // delimiter
                        break;
                    } else if (matcher.group(2) != null) {
                        // regular text blob
                        b.append(matcher.group(2));
                    } else if (matcher.group(3) != null) {
                        b.append(matcher.group(3));
                    } else {
                        // unreachable
                        throw new IllegalStateException();
                    }
                }
                list.add(b.toString());
                b.setLength(0);
            } else {
                // unreachable
                throw new IllegalStateException();
            }
        }
        if (item != null) {
            list.add(item);
        }
        return list.toArray(NO_STRINGS);
    }

    private static boolean isAsciiLetterOrDigit(char c) {
        return 'a' <= c && c <= 'z' ||
                'A' <= c && c <= 'Z' ||
                '0' <= c && c <= '9';
    }

    private static boolean isAsciiUpperCase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static char toAsciiLowerCase(char c) {
        return isAsciiUpperCase(c) ? (char) (c + 32) : c;
    }

    public static boolean equalsIgnoreCaseReplacingNonAlphanumericByUnderscores(final String envProperty,
            CharSequence dottedProperty) {
        int length = dottedProperty.length();
        if (envProperty.length() != dottedProperty.length()) {
            // special-case/slow-path
            if (length == 0 || envProperty.length() != dottedProperty.length() + 1) {
                return false;
            }
            if (dottedProperty.charAt(length - 1) == '"' &&
                    envProperty.charAt(length - 1) == '_' && envProperty.charAt(length) == '_') {
                length = dottedProperty.length() - 1;
            } else {
                return false;
            }
        }
        for (int i = 0; i < length; i++) {
            char ch = dottedProperty.charAt(i);
            if (!isAsciiLetterOrDigit(ch)) {
                if (envProperty.charAt(i) != '_') {
                    return false;
                }
                continue;
            }
            final char pCh = envProperty.charAt(i);
            // in theory property should be ascii too, but better play safe
            if (pCh < 128) {
                if (toAsciiLowerCase(pCh) != toAsciiLowerCase(ch)) {
                    return false;
                }
            } else if (Character.toLowerCase(envProperty.charAt(i)) != Character.toLowerCase(ch)) {
                return false;
            }
        }
        return true;
    }

    public static String replaceNonAlphanumericByUnderscores(final String name) {
        return replaceNonAlphanumericByUnderscores(name, new StringBuilder(name.length()));
    }

    public static String replaceNonAlphanumericByUnderscores(final String name, final StringBuilder sb) {
        int length = name.length();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (isAsciiLetterOrDigit(c)) {
                sb.append(c);
            } else {
                sb.append('_');
                if (c == '"' && i + 1 == length) {
                    sb.append('_');
                }
            }
        }
        return sb.toString();
    }

    public static String toLowerCaseAndDotted(final String name) {
        int length = name.length();

        if (length == 0) {
            return name;
        }

        byte[] result;
        if (length > 1 && name.charAt(length - 1) == '_' && name.charAt(length - 2) == '_') { // last quoted segment
            length--;
        }
        result = new byte[length];

        int i = 0;
        if (name.charAt(0) == '_') { // starting _ is a profile
            result[0] = '%';
            i++;
        }

        boolean quotesOpen = false;
        for (; i < length; i++) {
            char c = name.charAt(i);
            if ('_' == c) {
                int next = i + 1;
                if (quotesOpen) {
                    if (next == length) {
                        result[i] = '"'; // ending quotes
                    } else if (name.charAt(next) == '_') { // double _ end quote
                        result[i] = '"';
                        result[next] = '.';
                        i++;
                        quotesOpen = false;
                    } else {
                        result[i] = '.';
                    }
                } else if (next < length) {
                    char d = name.charAt(next);
                    if (Character.isDigit(d)) { // maybe index
                        result[next] = (byte) d;
                        int j = next + 1;
                        for (; j < length; j++) {
                            d = name.charAt(j);
                            if (Character.isDigit(d)) { // index
                                result[j] = (byte) d;
                            } else if ('_' == d) { // ending index
                                result[i] = '[';
                                result[j] = ']';
                                i = j;
                                break;
                            } else { // not an index
                                result[i] = '.';
                                i = j;
                                break;
                            }
                        }

                    } else if (name.charAt(next) == '_') { // double _ start quote
                        result[i] = '.';
                        result[next] = '"';
                        i++;
                        quotesOpen = true;
                    } else {
                        result[i] = '.';
                    }
                } else {
                    result[i] = '.';
                }
            } else {
                result[i] = (byte) Character.toLowerCase(c);
            }
        }

        // https://bugs.openjdk.org/browse/JDK-8295496
        return new String(result, 0, 0, result.length);
    }

    public static boolean isNumeric(final CharSequence digits) {
        return isNumeric(digits, 0, digits.length());
    }

    public static boolean isNumeric(final CharSequence digits, final int begin, final int end) {
        if (digits.length() == 0) {
            return false;
        }

        if (begin == end) {
            return false;
        }

        for (int i = begin; i < end; i++) {
            if (!Character.isDigit(digits.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String unquoted(final String name) {
        return unquoted(name, 0);
    }

    public static String unquoted(final String name, final int begin) {
        return unquoted(name, begin, name.length());
    }

    public static String unquoted(final String name, final int begin, final int end) {
        if (begin < 0 || begin > end || end > name.length()) {
            throw new StringIndexOutOfBoundsException("begin " + begin + ", end " + end + ", length " + name.length());
        }

        if (name.length() < 2) {
            return name;
        }

        if (name.charAt(begin) == '"' && name.charAt(end - 1) == '"') {
            return name.substring(begin + 1, end - 1);
        } else {
            return name.substring(begin, end);
        }
    }

    public static String unindexed(final String name) {
        if (name.length() < 3) {
            return name;
        }

        if (name.charAt(name.length() - 1) == ']') {
            int begin = name.lastIndexOf('[');
            if (begin != -1 && isNumeric(name, begin + 1, name.length() - 1)) {
                return name.substring(0, begin);
            }
        }

        return name;
    }

    public static String skewer(String camelHumps) {
        return skewer(camelHumps, '-');
    }

    public static String skewer(String camelHumps, char separator) {
        return skewer(camelHumps, 0, camelHumps.length(), new StringBuilder(), separator);
    }

    private static String skewer(String camelHumps, int start, int end, StringBuilder b, char separator) {
        if (camelHumps.isEmpty()) {
            throw new IllegalArgumentException("Method seems to have an empty name");
        }
        int cp = camelHumps.codePointAt(start);
        b.appendCodePoint(Character.toLowerCase(cp));
        start += Character.charCount(cp);
        if (start == end) {
            // a lonely character at the end of the string
            return b.toString();
        }
        if (Character.isUpperCase(cp)) {
            // all-uppercase words need one code point of lookahead
            int nextCp = camelHumps.codePointAt(start);
            if (Character.isUpperCase(nextCp)) {
                // it's some kind of `WORD`
                for (;;) {
                    b.appendCodePoint(Character.toLowerCase(nextCp));
                    start += Character.charCount(cp);
                    cp = nextCp;
                    if (start == end) {
                        return b.toString();
                    }
                    nextCp = camelHumps.codePointAt(start);
                    // combine non-letters in with this name
                    if (Character.isLowerCase(nextCp)) {
                        b.append(separator);
                        return skewer(camelHumps, start, end, b, separator);
                    }
                }
                // unreachable
            } else {
                // it was the start of a `Word`; continue until we hit the end or an uppercase.
                b.appendCodePoint(nextCp);
                start += Character.charCount(nextCp);
                for (;;) {
                    if (start == end) {
                        return b.toString();
                    }
                    cp = camelHumps.codePointAt(start);
                    // combine non-letters in with this name
                    if (Character.isUpperCase(cp)) {
                        b.append(separator);
                        return skewer(camelHumps, start, end, b, separator);
                    }
                    b.appendCodePoint(cp);
                    start += Character.charCount(cp);
                }
                // unreachable
            }
            // unreachable
        } else {
            // it's some kind of `word`
            for (;;) {
                cp = camelHumps.codePointAt(start);
                // combine non-letters in with this name
                if (Character.isUpperCase(cp)) {
                    b.append(separator);
                    return skewer(camelHumps, start, end, b, separator);
                }
                b.appendCodePoint(cp);
                start += Character.charCount(cp);
                if (start == end) {
                    return b.toString();
                }
            }
            // unreachable
        }
        // unreachable
    }
}
