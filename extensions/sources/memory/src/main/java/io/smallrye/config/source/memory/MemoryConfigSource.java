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
package io.smallrye.config.source.memory;

import io.smallrye.config.source.EnabledConfigSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.java.Log;

/**
 * In memory config source. Use the REST Endpoint to populate values
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
public class MemoryConfigSource extends EnabledConfigSource {
    
    public static final String NAME = "MemoryConfigSource";
    private static final Map<String,String> PROPERTIES = new ConcurrentHashMap<>();
    
    public MemoryConfigSource(){
        log.info("Loading [memory] MicroProfile ConfigSource");
        super.initOrdinal(900);
    }
    
    @Override
    public Map<String, String> getPropertiesIfEnabled() {
        return PROPERTIES;
    }

    @Override
    public String getValue(String key) {
        if(PROPERTIES.containsKey(key)){
            return PROPERTIES.get(key);
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}