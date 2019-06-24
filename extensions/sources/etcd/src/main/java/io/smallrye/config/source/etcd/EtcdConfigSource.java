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
package io.smallrye.config.source.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import io.smallrye.config.source.EnabledConfigSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Etcd config source
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 * 
 * TODO: Add watcher
 *          ByteSequence bsKey = ByteSequence.fromString(EMPTY);
 *          Watch.Watcher watch = this.client.getWatchClient().watch(bsKey);
 *          watch.listen();
 */
public class EtcdConfigSource extends EnabledConfigSource {
    private static final Logger log = Logger.getLogger(EtcdConfigSource.class.getName());
    
    private static final String NAME = "EtcdConfigSource";

    private Client client = null;
    private final long timeout;
    
    public EtcdConfigSource(){
        super.initOrdinal(320);
        timeout = loadTimeout();
    }
    
    @Override
    public Map<String, String> getPropertiesIfEnabled() {
        Map<String,String> m = new HashMap<>();
        
        ByteSequence bsKey = ByteSequence.fromString(EMPTY);

        CompletableFuture<GetResponse> getFuture = getClient().getKVClient().get(bsKey);
        try {
            GetResponse response = getFuture.get(timeout, TimeUnit.SECONDS);
            List<KeyValue> kvs = response.getKvs();

            for(KeyValue kv:kvs){
                String key = kv.getKey().toStringUtf8();
                String value = kv.getValue().toStringUtf8();
                m.put(key, value);
            }
        } catch (InterruptedException ex){
            Thread.currentThread().interrupt();
            log.log(Level.SEVERE, "Can not get all config keys and values from etcd Config source: {1}", new Object[]{ex.getMessage()});
        } catch (ExecutionException | TimeoutException ex) {
            log.log(Level.SEVERE, "Can not get all config keys and values from etcd Config source: {1}", new Object[]{ex.getMessage()});
        }
        
        return m;
    }

    @Override
    public String getValue(String key) {
        if (key.startsWith(KEY_PREFIX) || key.startsWith(getKeyWithPrefix(null))) {
            // in case we are about to configure ourselves we simply ignore that key
            return null;
        }
        if(super.isEnabled()){
            ByteSequence bsKey = ByteSequence.fromString(key);
            CompletableFuture<GetResponse> getFuture = getClient().getKVClient().get(bsKey);
            try {
                GetResponse response = getFuture.get(timeout, TimeUnit.SECONDS);
                String value = toString(response);
                return value;
            } catch (InterruptedException ex){
                Thread.currentThread().interrupt();
                log.log(Level.SEVERE, "Can not get config value for [{0}] from etcd Config source: {1}", new Object[]{key, ex.getMessage()});
            } catch (ExecutionException | TimeoutException ex){
                log.log(Level.SEVERE, "Can not get config value for [{0}] from etcd Config source: {1}", new Object[]{key, ex.getMessage()});
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
    
    private String toString(GetResponse response){
        if(response.getCount()>0){
            return response.getKvs().get(0).getValue().toStringUtf8();
        }
        return null;
    }
    
    private Client getClient(){
        if(this.client == null ){
            log.info("Loading [etcd] MicroProfile ConfigSource");

            String scheme = loadScheme();
            String host = loadHost();
            Integer port = loadPort();
            
            String user = loadUser();
            String password = loadPassword();
            String authority = loadAuthority();
            
            String endpoint = String.format("%s://%s:%d",scheme,host,port);
            log.log(Level.INFO, "Using [{0}] as etcd server endpoint", endpoint);
            
            ClientBuilder clientBuilder = Client.builder().endpoints(endpoint);
            if(user!=null){
                ByteSequence bsUser = ByteSequence.fromString(user);
                clientBuilder = clientBuilder.user(bsUser);
            }
            if(password!=null){
                ByteSequence bsPassword = ByteSequence.fromString(password);
                clientBuilder = clientBuilder.password(bsPassword);
            }
            if(authority!=null){
                clientBuilder = clientBuilder.authority(authority);
            }
            
            this.client = clientBuilder.build();
        }
        return this.client;
    }
    
    private String loadScheme(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_SCHEME), String.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_SCHEME), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_SCHEME)); 
    }
    
    private String loadHost(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_HOST), String.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_HOST), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_HOST)); 
    }
    
    private Integer loadPort(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_PORT), Integer.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_PORT), Integer.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_PORT)); 
    }
    
    private String loadUser(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_USER), String.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_USER), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(null)); 
    }
    
    private String loadPassword(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_PASSWORD), String.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_PASSWORD), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(null)); 
    }
    
    private String loadAuthority(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_AUTHORITY), String.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_AUTHORITY), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(null)); 
    }
    
    private Long loadTimeout(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_TIMEOUT), Long.class)
            .orElse(DEFAULT_TIMEOUT); 
    }
    
    @Deprecated
    private String getConfigKey(String subKey){
        return KEY_PREFIX + subKey;
    }
    
    private static final String KEY_PREFIX = "configsource.etcd.";
    
    private static final String KEY_SCHEME = "scheme";
    private static final String DEFAULT_SCHEME = "http";

    private static final String KEY_HOST = "host";
    private static final String DEFAULT_HOST = "localhost";

    private static final String KEY_PORT = "port";
    private static final Integer DEFAULT_PORT = 2379;

    private static final String KEY_USER = "user";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_AUTHORITY = "authority";
    
    private static final String KEY_TIMEOUT = "timeout";
    private static final Long DEFAULT_TIMEOUT = 10L;
    
    private static final String EMPTY = "";
}