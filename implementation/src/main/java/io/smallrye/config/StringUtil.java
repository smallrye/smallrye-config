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

package io.smallrye.config;

import java.util.regex.Pattern;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class StringUtil {

    // delimiter is a comma that is not preceded by a \
    private static final Pattern DELIMITER = Pattern.compile("(?<!\\\\),+");
    private static final String[] NO_STRINGS = new String[0];
    private static final Pattern ESCAPED_COMMA = Pattern.compile("\\\\,");
    private static final Pattern LEADING_COMMAS = Pattern.compile("^,+");

    public static String[] split(String text) {
        if (text == null || text.isEmpty()) {
            return NO_STRINGS;
        }
        text = LEADING_COMMAS.matcher(text).replaceAll("");
        String[] split = DELIMITER.split(text);
        if (split.length == 0 || split.length == 1 && split[0].isEmpty()) {
            return NO_STRINGS;
        }
        for (int i = 0 ;i < split.length ; i++) {
            split[i] = ESCAPED_COMMA.matcher(split[i]).replaceAll(",");
        }
        return split;
    }
}
