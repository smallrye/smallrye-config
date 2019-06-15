/*
 * Copyright 2019 Red Hat, Inc.
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
package io.smallrye.ext.config.source.base;

import java.util.logging.Level;
import lombok.Getter;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Base class for all our ConfigSources (Originally from Geronimo Config)
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 * @author <a href="mailto:dpmoore@acm.org">Derek P. Moore</a>
 */
@Log
public abstract class BaseConfigSource implements ConfigSource {
    
    @Getter
    private final Config config;
    private int ordinal = 1000; // default

    public BaseConfigSource(){
        super();
        this.config = createConfig();
    }
    
    @Override
    public int getOrdinal() {
        return ordinal;
    }

    /**
     * Init method e.g. for initializing the ordinal.
     * This method can be used from a subclass to determine
     * the ordinal value
     *
     * @param defaultOrdinal the default value for the ordinal if not set via configuration
     */
    protected void initOrdinal(int defaultOrdinal) {
        ordinal = defaultOrdinal;

        String configuredOrdinalString = getValue(CONFIG_ORDINAL);

        try {
            if (configuredOrdinalString != null) {
                ordinal = Integer.parseInt(configuredOrdinalString.trim());
            }
        } catch (NumberFormatException e) {
            log.log(Level.WARNING, e,
                    () -> "The configured config-ordinal isn't a valid integer. Invalid value: " + configuredOrdinalString);
        }
    }

    private Config createConfig(){
        return ConfigProviderResolver.instance()
            .getBuilder()
            .addDefaultSources()
            .build();
    }
    
}
