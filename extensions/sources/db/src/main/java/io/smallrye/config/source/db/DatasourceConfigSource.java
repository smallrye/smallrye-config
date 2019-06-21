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
package io.smallrye.config.source.db;

import io.smallrye.config.events.ChangeEvent;
import io.smallrye.config.events.ChangeEventNotifier;
import io.smallrye.config.events.Type;
import io.smallrye.config.source.EnabledConfigSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import lombok.extern.java.Log;
import org.eclipse.microprofile.config.Config;

@Log
public class DatasourceConfigSource extends EnabledConfigSource {
    private static final String NAME = "DatasourceConfigSource";
    
    private final Map<String, TimedEntry> cache = new ConcurrentHashMap<>();
    Repository repository = null;
    private Long validity = null;
    private final boolean notifyOnChanges;
    
    private final String datasource;
    private final String table;
    private final String keyColumn;
    private final String valueColumn;
        
    private final Config config;
    
    public DatasourceConfigSource() {
        log.info("Loading [db] MicroProfile ConfigSource");
        this.config = getConfig();
        this.notifyOnChanges = loadNotifyOnChanges();
        
        this.datasource = loadDatasource();
        this.table = loadTable();
        this.keyColumn = loadKeyColumn();
        this.valueColumn = loadValueColumn();
        
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
            repository = new Repository(datasource,table,keyColumn,valueColumn);
        }
    }
    
    private void initValidity(){
        if (validity == null) {
            validity = config.getOptionalValue(getKeyWithPrefix(VALIDITY), Long.class)
            .orElse(config.getOptionalValue(getConfigKey(VALIDITY), Long.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAUTL_VALIDITY));
        }
    }
    
    @Deprecated
    private String getConfigKey(String subKey){
        return "configsource.db." + subKey;
    }
    
    private boolean loadNotifyOnChanges(){
        return config.getOptionalValue(getKeyWithPrefix(NOTIFY_ON_CHANGES), Boolean.class)
            .orElse(config.getOptionalValue(getConfigKey(NOTIFY_ON_CHANGES), Boolean.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_NOTIFY_ON_CHANGES)); 
    }
    
    private String loadDatasource(){
        return config.getOptionalValue(getKeyWithPrefix(DATASOURCE), String.class)
            .orElse(config.getOptionalValue(getConfigKey(DATASOURCE), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_DATASOURCE)); 
    }
    
    private String loadTable(){
        return config.getOptionalValue(getKeyWithPrefix(TABLE), String.class)
            .orElse(config.getOptionalValue(getConfigKey(TABLE), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_TABLE)); 
    }   
    
    private String loadKeyColumn(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_COL), String.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_COL), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_KEY_COL)); 
    }
    
    private String loadValueColumn(){
        return config.getOptionalValue(getKeyWithPrefix(VAL_COL), String.class)
            .orElse(config.getOptionalValue(getConfigKey(VAL_COL), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_VAL_COL)); 
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
    
    private static final String VALIDITY = "validity";
    private static final long DEFAUTL_VALIDITY = 30000L;
    
    private static final String NOTIFY_ON_CHANGES = "notifyOnChanges";
    private static final boolean DEFAULT_NOTIFY_ON_CHANGES = true;
    
    private static final String DATASOURCE = "datasource";
    private static final String DEFAULT_DATASOURCE = "java:comp/DefaultDataSource";

    private static final String TABLE = "table";
    private static final String DEFAULT_TABLE = "configuration";
    
    private static final String KEY_COL = "key-column";
    private static final String DEFAULT_KEY_COL = "key";
    
    private static final String VAL_COL = "value";
    private static final String DEFAULT_VAL_COL = "value-column";
    
}
