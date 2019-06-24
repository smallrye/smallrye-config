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
package io.smallrye.config.source;

import java.util.logging.Level;
import java.util.logging.Logger;
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
public abstract class SelfConfiguringConfigSource implements ConfigSource {
    private static final Logger log = Logger.getLogger(SelfConfiguringConfigSource.class.getName());
    
    protected final Config config;
    private int ordinal = 1000; // default

    public SelfConfiguringConfigSource(){
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

    protected String getKeyWithPrefix(String key){
        String prefix = getClass().getPackage().getName() + DOT;
        if(key==null)return prefix;
        return prefix + key;
    }
    
    private Config createConfig(){
        return ConfigProviderResolver.instance()
            .getBuilder()
            .addDefaultSources()
            .build();
    }
    
    protected static final String DOT= ".";
}
