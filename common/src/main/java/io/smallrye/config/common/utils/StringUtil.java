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

    public static String replaceNonAlphanumericByUnderscores(final String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if ('a' <= c && c <= 'z' ||
                    'A' <= c && c <= 'Z' ||
                    '0' <= c && c <= '9') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    public static String toLowerCaseAndDotted(final String name) {
        int length = name.length();
        int beginSegment = 0;
        boolean quotesOpen = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if ('_' == c) {
                if (i == 0) {
                    // leading _ can only mean a profile
                    sb.append("%");
                    continue;
                }

                // Do not convert to index if the first segment is a number
                if (beginSegment > 0) {
                    try {
                        String segment = sb.substring(beginSegment, i);
                        Integer.parseInt(segment);
                        sb.replace(beginSegment - 1, beginSegment, "[").append("]");

                        int j = i + 1;
                        if (j < length) {
                            if ('_' == name.charAt(j)) {
                                sb.append(".");
                                i = j;
                            }
                        }

                        continue;
                    } catch (NumberFormatException e) {
                        // Ignore, it is not an indexed number
                    }
                }

                int j = i + 1;
                if (j < length) {
                    if ('_' == name.charAt(j) && !quotesOpen) {
                        sb.append(".");
                        sb.append("\"");
                        i = j;
                        quotesOpen = true;
                    } else if ('_' == name.charAt(j) && quotesOpen) {
                        sb.append("\"");
                        sb.append(".");
                        i = j;
                        quotesOpen = false;
                    } else {
                        sb.append(".");
                    }
                } else {
                    sb.append(".");
                }
                beginSegment = j;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
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
