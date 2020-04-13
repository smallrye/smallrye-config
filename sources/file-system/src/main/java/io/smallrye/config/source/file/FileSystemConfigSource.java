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

package io.smallrye.config.source.file;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Read configuration from a file directory.
 * <p>
 * Each file in the directory corresponds to a property where the file name is the property key
 * and the file textual content is the property value.
 * <p>
 * For example, if a directory structure looks like:
 *
 * <pre>
 * <code>
 * foo/
 * ├── num.max
 * └── num.size
 * </code>
 * </pre>
 *
 * <code>new DirConfigSource("foo")</code> will provide 2 properties:
 * <ul>
 * <li><code>num.max</code></li>
 * <li><code>num.size</code></li>
 * </ul>
 * <p>
 * Nested directories are not supported.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class FileSystemConfigSource extends MapBackedConfigSource {
    private static final long serialVersionUID = 654034634846856045L;

    private static final Pattern PATTERN = Pattern.compile("[^a-zA-Z0-9_]");
    private static final Logger LOG = Logger.getLogger("io.smallrye.config");

    FileSystemConfigSource(File dir) {
        this(dir, DEFAULT_ORDINAL);
    }

    /**
     * Construct a new instance
     *
     * @param dir the directory, containing configuration files
     * @param ordinal the ordinal value
     */
    public FileSystemConfigSource(File dir, int ordinal) {
        super("DirConfigSource[dir=" + dir.getAbsolutePath() + "]", scan(dir), ordinal);
    }

    private static Map<String, String> scan(File directory) {
        if (directory != null && directory.isDirectory()) {
            try (Stream<Path> stream = Files.walk(directory.toPath(), 1)) {

                return stream.filter(p -> p.toFile().isFile())
                        .collect(Collectors.toMap(it -> it.getFileName().toString(), FileSystemConfigSource::readContent));
            } catch (Exception e) {
                LOG.warnf("Unable to read content from file %s. Exception: %s", directory.getAbsolutePath(),
                        e.getLocalizedMessage());
            }
        }
        return Collections.emptyMap();
    }

    private static String readContent(Path file) {
        try (Stream<String> stream = Files.lines(file)) {
            return stream.collect(Collectors.joining());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getValue(String name) {
        if (name == null) {
            return null;
        }

        final Map<String, String> properties = getProperties();

        // exact match
        String value = properties.get(name);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores
        String sanitizedName = PATTERN.matcher(name).replaceAll("_");

        value = properties.get(sanitizedName);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores and convert to uppercase
        return properties.get(sanitizedName.toUpperCase());
    }
}
