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
package io.smallrye.ext.config.source.db;

import io.smallrye.ext.config.events.ChangeEvent;
import io.smallrye.ext.config.events.ChangeEventNotifier;
import io.smallrye.ext.config.events.Type;
import io.smallrye.ext.config.source.base.EnabledConfigSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import lombok.extern.java.Log;

@Log
public class DatasourceConfigSource extends EnabledConfigSource {
    private static final String NAME = "DatasourceConfigSource";
    
    private final Map<String, TimedEntry> cache = new ConcurrentHashMap<>();
    Repository repository = null;
    private Long validity = null;
    private final boolean notifyOnChanges;
    
    public DatasourceConfigSource() {
        log.info("Loading [db] MicroProfile ConfigSource");
        this.notifyOnChanges = loadNotifyOnChanges();
        super.initOrdinal(120);
    }

    @Override
    public Map<String, String> getPropertiesIfEnabled() {
        initRepository();
        return repository.getAllConfigValues();
    }

    @Override
    public String getValue(String propertyName) {
        if (CONFIG_ORDINAL.equals(propertyName)) {
            return null;
        }
        
        initRepository();
        initValidity();
        
        // TODO: Cache null values ? So if the config is not in the DB, cache the empty value
        // TODO: If not first time load, fire NEW Event ?
        TimedEntry entry = cache.get(propertyName);
        
        if (entry == null){
            // First time read (Or no config)
            return readFromDB(propertyName);
        }else if(entry.isExpired()) {
            // Time to refresh
            ChangeEventNotifier changeEventNotifier = ChangeEventNotifier.getInstance();
            String oldValue = entry.getValue();
            String newValue = readFromDB(propertyName);
            if(notifyOnChanges){
                // Remove Event
                if(newValue==null){
                    changeEventNotifier.fire(new ChangeEvent(Type.REMOVE,propertyName,changeEventNotifier.getOptionalOldValue(oldValue),null,NAME));
                // Change Event
                }else if(!oldValue.equals(newValue)){
                    changeEventNotifier.fire(new ChangeEvent(Type.UPDATE,propertyName,changeEventNotifier.getOptionalOldValue(oldValue),newValue,NAME));
                }
            }
            
            return newValue;
        }else{
            // Read from cache
            return entry.getValue();
        }
        
    }

    @Override
    public String getName() {
        return NAME;
    }

    private String readFromDB(String propertyName){
        log.log(Level.FINE, () -> "load " + propertyName + " from db");
        String value = repository.getConfigValue(propertyName);
        cache.put(propertyName, new TimedEntry(value));
        return value;
    }
    
    private void initRepository(){
        if (repository == null) {
            // late initialization is needed because of the EE datasource.
            repository = new Repository(getConfig());
        }
    }
    
    private void initValidity(){
        if (validity == null) {
            validity = getConfig().getOptionalValue("configsource.db.validity", Long.class).orElse(30000L);
        }
    }
    
    private String getConfigKey(String subKey){
        return "configsource.db." + subKey;
    }
    
    private boolean loadNotifyOnChanges(){
        return getConfig().getOptionalValue(getConfigKey(NOTIFY_ON_CHANGES), Boolean.class).orElse(DEFAULT_NOTIFY_ON_CHANGES);
    }
    
    class TimedEntry {
        private final String value;
        private final long timestamp;

        public TimedEntry(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            return (timestamp + validity) < System.currentTimeMillis();
        }
    }
    
    private static final String NOTIFY_ON_CHANGES = "notifyOnChanges";
    private static final boolean DEFAULT_NOTIFY_ON_CHANGES = true;
}
