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
package io.smallrye.config.example;

import io.smallrye.config.events.ChangeEvent;
import io.smallrye.config.events.KeyFilter;
import io.smallrye.config.events.SourceFilter;
import io.smallrye.config.events.Type;
import io.smallrye.config.events.TypeFilter;
import io.smallrye.config.events.regex.Field;
import io.smallrye.config.events.regex.RegexFilter;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.EventMetadata;
import lombok.extern.java.Log;

/**
 * Example Listener for changes in the events
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
@ApplicationScoped
public class EventChangeListener {

    // Getting all config event
    public void all(@Observes ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL: Received a config change event: {0}", changeEvent);
    }
    
    // Get only new values
    public void newValue(@Observes @TypeFilter(Type.NEW) ChangeEvent changeEvent){
        log.log(Level.SEVERE, "NEW: Received a config change event: {0}", changeEvent);
    }
    
    // Get only override values
    public void overrideValue(@Observes @TypeFilter(Type.UPDATE) ChangeEvent changeEvent){
        log.log(Level.SEVERE, "UPDATE: Received a config change event: {0}", changeEvent);
    }
    
    // Get only revert values
    public void revertValue(@Observes @TypeFilter(Type.REMOVE) ChangeEvent changeEvent){
        log.log(Level.SEVERE, "REMOVE: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key
    public void allForKey(@Observes @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    @RegexFilter("^some\\..+") // Starting with some.
    public void allForPatternMatchOnKey(@Observes ChangeEvent changeEvent, EventMetadata meta){
        log.log(Level.SEVERE, "Pattern match on key: Received a config change event: {0}", changeEvent);
    }
    
    @RegexFilter(onField = Field.oldValue, value = "^some\\..+") // Starting with some.
    public void allForPatternMatchOnOldValue(@Observes ChangeEvent changeEvent, EventMetadata meta){
        log.log(Level.SEVERE, "Pattern match on old value: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key for new events
    public void newForKey(@Observes @TypeFilter(Type.NEW) @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "NEW for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key for override events
    public void overrideForKey(@Observes @TypeFilter(Type.UPDATE) @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "UPDATE for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key for revert events
    public void revertForKey(@Observes @TypeFilter(Type.REMOVE) @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "REMOVE for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config events for a certain source
    public void allForSource(@Observes @SourceFilter("MemoryConfigSource") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL for source [MemoryConfigSource]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config events for a certain source
    public void allForSourceAndKey(@Observes @SourceFilter("MemoryConfigSource") @KeyFilter("some.key")  ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL for source [MemoryConfigSource] and for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config events for a certain source
    public void overrideForSourceAndKey(@Observes @TypeFilter(Type.UPDATE) @SourceFilter("MemoryConfigSource") @KeyFilter("some.key")  ChangeEvent changeEvent){
        log.log(Level.SEVERE, "UPDATE for source [MemoryConfigSource] and for key [some.key]: Received a config change event: {0}", changeEvent);
    }
}
