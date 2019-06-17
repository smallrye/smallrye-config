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
package io.smallrye.ext.config.providers;

import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Making the Config sources available via CDI
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
@ApplicationScoped
public class ConfigSourceMapProvider {
    @Inject
    private Provider<Config> configProvider;
    
    private final Map<String,ConfigSource> configSourceMap = new HashMap<>();
    
    @Produces @ConfigSourceMap
    public Map<String,ConfigSource> produceConfigSourceMap(){
        if(this.configSourceMap.isEmpty()){
            for(ConfigSource configSource:configProvider.get().getConfigSources()){
                this.configSourceMap.put(configSource.getName(), configSource);
            }
        }
        return this.configSourceMap;
    }
}
