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

    // this is accounting for Latin1 chars only
    private static final byte[] NON_ALPHANUMERIC_UNDERSCORE_REPLACEMENTS = new byte[256];

    static {
        // replace every non alpha-numeric latin char by an underscore
        for (int c = 0; c < 256; c++) {
            if ('a' <= c && c <= 'z' ||
                    'A' <= c && c <= 'Z' ||
                    '0' <= c && c <= '9') {
                NON_ALPHANUMERIC_UNDERSCORE_REPLACEMENTS[c] = (byte) c;
            } else {
                NON_ALPHANUMERIC_UNDERSCORE_REPLACEMENTS[c] = '_';
            }
        }
    }

    public static boolean isAsciiLetterOrDigit(char c) {
        if (c > 255) {
            return false;
        }
        return NON_ALPHANUMERIC_UNDERSCORE_REPLACEMENTS[c & 0xFF] != '_';
    }

    private static char replacementOf(char c) {
        if (c > 255) {
            return '_';
        }
        return (char) (((int) NON_ALPHANUMERIC_UNDERSCORE_REPLACEMENTS[c & 0xFF]) & 0xFF);
    }

    private static byte rawReplacementOf(char c) {
        if (c > 255) {
            return '_';
        }
        return NON_ALPHANUMERIC_UNDERSCORE_REPLACEMENTS[c & 0xFF];
    }

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

    public static String replaceNonAlphanumericByUnderscores(final String name) {
        // size it accounting for worst case scenario
        byte[] usAsciiResult = new byte[name.length() + 1];
        int length = name.length();
        // bogus value
        char c = 0;
        for (int i = 0; i < length; i++) {
            c = name.charAt(i);
            usAsciiResult[i] = rawReplacementOf(c);
        }
        if (c == '"') {
            usAsciiResult[length] = '_';
            return new String(usAsciiResult, 0, 0, usAsciiResult.length);
        } else {
            return new String(usAsciiResult, 0, 0, usAsciiResult.length - 1);
        }
    }

    public static String replaceNonAlphanumericByUnderscores(final String name, final StringBuilder sb) {
        int length = name.length();
        // bogus value
        char c = 0;
        for (int i = 0; i < length; i++) {
            c = name.charAt(i);
            sb.append(replacementOf(c));
        }
        if (c == '"') {
            sb.append('_');
        }
        return sb.toString();
    }

    public static final class ResizableByteArray {

        private byte[] array;

        public ResizableByteArray(int initialSize) {
            this.array = new byte[initialSize];
        }

        public void set(int index, byte value) {
            array[index] = value;
        }

        public void ensureCapacity(int capacity) {
            if (array.length < capacity) {
                byte[] newArray = new byte[capacity];
                System.arraycopy(array, 0, newArray, 0, array.length);
                array = newArray;
            }
        }

        public String toUsAsciiString(int length) {
            return new String(array, 0, 0, length);
        }
    }

    public static String replaceNonAlphanumericByUnderscores(final String name, final ResizableByteArray sb) {
        // size it accounting for worst case scenario
        int length = name.length();
        sb.ensureCapacity(length + 1);
        // bogus value
        char c = 0;
        for (int i = 0; i < length; i++) {
            c = name.charAt(i);
            sb.set(i, rawReplacementOf(c));
        }
        if (c == '"') {
            sb.set(length, (byte) '_');
            return sb.toUsAsciiString(length + 1);
        } else {
            return sb.toUsAsciiString(length);
        }
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
        if (name.charAt(0) == '_') {
            if (name.length() > 1 && isAsciiLetterOrDigit(name.charAt(1))) { // starting single _ is a profile
                result[0] = '%';
                i++;
            }
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

    /**
     * Matches if a dotted property name is part of a dotted path.
     *
     * @param path the dotted path
     * @param name a dotted property name
     * @return <code>true</code> if the dotted property name ir part of a dotted path, or <code>false</code> otherwise.
     */
    public static boolean isInPath(final String path, final String name) {
        if (name.equals(path)) {
            return true;
        }

        // if property is less than the root no way to match
        if (name.length() <= path.length()) {
            return false;
        }

        // foo.bar
        // foo.bar."baz"
        // foo.bar[0]
        char e = name.charAt(path.length());
        if ((e == '.') || e == '[') {
            for (int i = 0; i < path.length(); i++) {
                char r = path.charAt(i);
                e = name.charAt(i);
                if (r == '-') {
                    if (e != '.' && e != '-') {
                        return false;
                    }
                } else if (r != e) {
                    return false;
                }
            }
            return true;
        }
        return false;
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

        if (name.length() < 2 || name.length() <= begin) {
            return name;
        }

        if (name.charAt(begin) == '"' && name.charAt(end - 1) == '"') {
            return name.substring(begin + 1, end - 1);
        } else {
            return name.substring(begin, end);
        }
    }

    public static int index(final String name) {
        if (name.charAt(name.length() - 1) == ']') {
            int start = name.lastIndexOf('[');
            if (start != -1 && isNumeric(name, start + 1, name.length() - 1)) {
                return Integer.parseInt(name.substring(start + 1, name.length() - 1));
            }
        }
        throw new IllegalArgumentException();
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
        if (camelHumps.isEmpty()) {
            return camelHumps;
        }

        int end = camelHumps.length();
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < end; i++) {
            char c = camelHumps.charAt(i);
            if (Character.isLowerCase(c)) {
                b.append(c);
            } else if (Character.isUpperCase(c)) {
                if (i > 0) {
                    char last = camelHumps.charAt(i - 1);
                    if (last != '_' && last != '-') {
                        b.append(separator);
                    }
                }
                b.append(Character.toLowerCase(c));
                // lookahead for all upper case words, like fooBAR transform to foo_bar and not foo_b_a_r
                // move caret by 1
                int j = i + 1;
                for (; j < end; j++) {
                    char u = camelHumps.charAt(j);
                    if (Character.isUpperCase(u)) {
                        b.append(Character.toLowerCase(u));
                    } else if (Character.isDigit(u) || u == '-') {
                        // A digit in the middle will break the all upper case word, the main cycle can resume
                        b.append(u);
                    } else {
                        // it is an all upper case word if j > i + 1, the initial value
                        if (j > i + 1 && u != '_' && !Character.isDigit(b.charAt(b.length() - 1))) {
                            // all upper case word done, but last upper starts a new word, so we need to insert the separator
                            b.insert(b.length() - 1, separator);
                        }
                        // we don't know what is coming next, so we go back and let the main cycle handle it
                        j--;
                        break;
                    }
                }
                // restore caret
                i = j;
            } else if (Character.isDigit(c)) {
                b.append(c);
            } else if (c == '.' || c == '*' || c == '[' || c == ']') {
                b.append(c);
            } else {
                if (i > 0) {
                    char last = camelHumps.charAt(i - 1);
                    if (last != '_' && last != '-') {
                        b.append(separator);
                    }
                } else {
                    b.append(c);
                }
            }
        }

        return b.toString();
    }
}
