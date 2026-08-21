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

import static io.smallrye.config.common.utils.ConfigSourceUtil.propertiesToMap;
import static java.util.Collections.unmodifiableMap;

import java.io.Serial;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SysPropConfigSource extends AbstractConfigSource {
    @Serial
    private static final long serialVersionUID = 9167738611308785403L;

    public static final String NAME = "SysPropConfigSource";
    public static final int ORDINAL = 400;

    public SysPropConfigSource() {
        super(NAME, getSystemOrdinal());
    }

    @Override
    public Map<String, String> getProperties() {
        return unmodifiableMap(propertiesToMap(System.getProperties()));
    }

    @Override
    public Set<String> getPropertyNames() {
        return System.getProperties().stringPropertyNames();
    }

    @Override
    public String getValue(String propertyName) {
        return System.getProperty(propertyName);
    }

    private static int getSystemOrdinal() {
        String value = System.getProperty(ConfigSourceUtil.CONFIG_ORDINAL_KEY);
        if (value != null) {
            return Converters.INTEGER_CONVERTER.convert(value);
        }
        return ORDINAL;
    }
}
