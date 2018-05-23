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

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class StringUtil {

    // delimiter is a comma that is not preceded by a \
    private static final String DELIMITER = "(?<!\\\\),";

    public static String[] split(String text) {
        if (text == null) {
            return new String[0];
        }
        String[] split = text.split(DELIMITER);
        for (int i = 0 ;i < split.length ; i++) {
            split[i] = split[i].replace("\\,", ",");
        }
        return split;
    }
}
