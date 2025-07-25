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

import static io.smallrye.config.common.utils.ConfigSourceUtil.CONFIG_ORDINAL_KEY;
import static io.smallrye.config.common.utils.ConfigSourceUtil.propertiesToMap;
import static java.security.AccessController.doPrivileged;
import static java.util.Collections.unmodifiableMap;

import java.io.Serial;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;

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
        if (System.getSecurityManager() == null) {
            return unmodifiableMap(propertiesToMap(System.getProperties()));
        } else {
            return doPrivileged(new PrivilegedAction<Map<String, String>>() {
                @Override
                public Map<String, String> run() {
                    return unmodifiableMap(propertiesToMap(doPrivileged((PrivilegedAction<Properties>) System::getProperties)));
                }
            });
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        if (System.getSecurityManager() == null) {
            return System.getProperties().stringPropertyNames();
        } else {
            return doPrivileged(new PrivilegedAction<Set<String>>() {
                @Override
                public Set<String> run() {
                    return System.getProperties().stringPropertyNames();
                }
            });
        }
    }

    @Override
    public String getValue(String propertyName) {
        return getSystemProperty(propertyName);
    }

    private static int getSystemOrdinal() {
        String value = getSystemProperty(CONFIG_ORDINAL_KEY);
        if (value != null) {
            return Converters.INTEGER_CONVERTER.convert(value);
        }
        return ORDINAL;
    }

    private static String getSystemProperty(final String propertyName) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(propertyName);
        } else {
            return doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(propertyName);
                }
            });
        }
    }
}
