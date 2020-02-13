/*
 * Copyright 2020 Red Hat, Inc.
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

package io.smallrye.config.source.kubernetes;

import io.smallrye.config.source.yaml.YamlConfigSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

public class YamlConfigSourceParser implements ConfigSourceParser {

    private static final Logger LOG = Logger.getLogger("io.smallrye.config");

    @Override
    public ConfigSource parser(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            return new YamlConfigSource(path.getFileName().toString(), stream);
        } catch (IOException e) {
            LOG.warnf("Unable to read content from file %s. Exception: %s", path.toAbsolutePath(),
                e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public boolean isSupported(Path path) {
        return isFileExtension(path, ".yaml") || isFileExtension(path, ".yml");
    }
}
