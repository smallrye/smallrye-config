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
package io.smallrye.config.providers;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Making the Config sources available via CDI
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
@Dependent
public class ConfigSourceProvider {

    @Inject @ConfigSourceMap
    private Map<String,ConfigSource> configSourceMap;
    
    @Produces @Name("")
    public ConfigSource produceConfigSource(final InjectionPoint injectionPoint) {
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        String name = getName(qualifiers);
        return configSourceMap.get(name);
    }
    
    private String getName(Set<Annotation> qualifiers){
        for(Annotation qualifier:qualifiers){
            if(qualifier.annotationType().equals(Name.class)){
                Name name = (Name)qualifier;
                return name.value();
            }
        }
        return "";
    }
}
