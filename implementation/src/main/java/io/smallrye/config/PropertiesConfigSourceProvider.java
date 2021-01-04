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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class PropertiesConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
    private final List<ConfigSource> configSources = new ArrayList<>();
    private final boolean includeFileSystem;

    public PropertiesConfigSourceProvider(final String location, final ClassLoader classLoader) {
        this(location, classLoader, true);
    }

    public PropertiesConfigSourceProvider(final String location, final ClassLoader classLoader,
            final boolean includeFileSystem) {
        this.includeFileSystem = includeFileSystem;
        this.configSources.addAll(loadConfigSources(location, classLoader));
    }

    @Deprecated
    public PropertiesConfigSourceProvider(String location, boolean optional, ClassLoader classLoader) {
        this(location, classLoader);
        if (!optional && this.configSources.isEmpty()) {
            throw ConfigMessages.msg.fileNotFound(location);
        }
    }

    @Override
    public List<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return configSources;
    }

    @Override
    protected String[] getFileExtensions() {
        return new String[] { "properties" };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new PropertiesConfigSource(url, ordinal);
    }

    @Override
    protected List<ConfigSource> tryFileSystem(final URI uri) {
        if (includeFileSystem) {
            return super.tryFileSystem(uri);
        } else {
            return new ArrayList<>();
        }
    }

    public static PropertiesConfigSourceProvider resource(final String location, final ClassLoader classLoader) {
        return new PropertiesConfigSourceProvider(location, classLoader);
    }

    public static PropertiesConfigSourceProvider classPathResource(final String location, final ClassLoader classLoader) {
        return new PropertiesConfigSourceProvider(location, classLoader, false);
    }
}
