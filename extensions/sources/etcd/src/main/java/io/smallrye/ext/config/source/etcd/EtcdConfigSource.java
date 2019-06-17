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
package io.smallrye.ext.config.source.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import io.smallrye.ext.config.source.base.EnabledConfigSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import lombok.extern.java.Log;

/**
 * Etcd config source
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 * 
 * TODO: Add watcher
 *          ByteSequence bsKey = ByteSequence.fromString(EMPTY);
 *          Watch.Watcher watch = this.client.getWatchClient().watch(bsKey);
 *          watch.listen();
 */
@Log
public class EtcdConfigSource extends EnabledConfigSource {
    
    private static final String NAME = "EtcdConfigSource";

    private static final String KEY_PREFIX = "configsource.etcd.";
    
    private static final String KEY_SCHEME = KEY_PREFIX + "scheme";
    private static final String DEFAULT_SCHEME = "http";

    private static final String KEY_HOST = KEY_PREFIX + "host";
    private static final String DEFAULT_HOST = "localhost";

    private static final String KEY_PORT = KEY_PREFIX + "port";
    private static final Integer DEFAULT_PORT = 2379;

    private static final String KEY_USER = KEY_PREFIX + "user";
    private static final String KEY_PASSWORD = KEY_PREFIX + "password";
    private static final String KEY_AUTHORITY = KEY_PREFIX + "authority";
    
    private Client client = null;

    public EtcdConfigSource(){
        super.initOrdinal(320);
    }
    
    @Override
    public Map<String, String> getPropertiesIfEnabled() {
        Map<String,String> m = new HashMap<>();
        
        ByteSequence bsKey = ByteSequence.fromString(EMPTY);

        CompletableFuture<GetResponse> getFuture = getClient().getKVClient().get(bsKey);
        try {
            GetResponse response = getFuture.get();
            List<KeyValue> kvs = response.getKvs();

            for(KeyValue kv:kvs){
                String key = kv.getKey().toStringUtf8();
                String value = kv.getValue().toStringUtf8();
                m.put(key, value);
            }
        } catch (InterruptedException | ExecutionException ex) {
            log.log(Level.FINEST, "Can not get all config keys and values from etcd Config source: {1}", new Object[]{ex.getMessage()});
        }
        
        return m;
    }

    @Override
    public String getValue(String key) {
        if (key.startsWith(KEY_PREFIX)) {
            // in case we are about to configure ourselves we simply ignore that key
            return null;
        }
        if(super.isEnabled()){
            ByteSequence bsKey = ByteSequence.fromString(key);
            CompletableFuture<GetResponse> getFuture = getClient().getKVClient().get(bsKey);
            try {
                GetResponse response = getFuture.get();
                String value = toString(response);
                return value;
            } catch (InterruptedException | ExecutionException ex) {
                log.log(Level.FINEST, "Can not get config value for [{0}] from etcd Config source: {1}", new Object[]{key, ex.getMessage()});
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

            String scheme = getConfig().getOptionalValue(KEY_SCHEME, String.class).orElse(DEFAULT_SCHEME);
            String host = getConfig().getOptionalValue(KEY_HOST, String.class).orElse(DEFAULT_HOST);
            Integer port = getConfig().getOptionalValue(KEY_PORT, Integer.class).orElse(DEFAULT_PORT);
            
            String user = getConfig().getOptionalValue(KEY_USER, String.class).orElse(null);
            String password = getConfig().getOptionalValue(KEY_PASSWORD, String.class).orElse(null);
            String authority = getConfig().getOptionalValue(KEY_AUTHORITY, String.class).orElse(null);
            
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
    
    private static final String EMPTY = "";

}
