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
package io.smallrye.config.source.consul;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.text.StringSubstitutor;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;
import io.smallrye.config.source.EnabledConfigSource;

import lombok.extern.java.Log;
import org.eclipse.microprofile.config.Config;

/**
 * Consul config source
 * @author Arik Dasen
 */
@Log
public class ConsulConfigSource extends EnabledConfigSource {
    
    private static final String NAME = "ConsulConfigSource";

    // enable variable substitution for configsource config (e.g. consul host might be injected by the environment, but with a different key)
    // TODO verify if still needed
    private final StringSubstitutor substitutor = new StringSubstitutor(s -> getConfig().getOptionalValue(s, String.class).orElse(""));

    ConsulClient client = null;
    private Long validity = null;
    private String prefix = null;
    private String host = null;
    
    private final Map<String, TimedEntry> cache = new ConcurrentHashMap<>();
    private final Config config;
    
    public ConsulConfigSource() {
        super.initOrdinal(320);
        this.config = getConfig();
    }
    
    // used for tests
    public ConsulConfigSource(ConsulClient client) {
        super.initOrdinal(320);
        this.client = client;
        this.config = getConfig();
    }

    @Override
    public Map<String, String> getPropertiesIfEnabled() {
        log.info("getProperties");
        return cache.entrySet()
                .stream()
                .filter(e -> e.getValue().getValue() != null)
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().getValue()));
    }

    @Override
    public String getValue(String key) {
        if (key.startsWith(KEY_PREFIX) || key.startsWith(getKeyWithPrefix(null))) {
            // in case we are about to configure ourselves we simply ignore that key
            return null;
        }
        
        TimedEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            log.log(Level.FINE, "load {0} from consul", key);
            GetValue value = null;
            try {
                value = getClient().getKVValue(getPrefix() + key).getValue();
            } catch (Exception e) {
                log.log(Level.WARNING, "consul getKVValue failed: {0}", e.getMessage());
                if (entry != null) {
                    return entry.getValue();
                }
            }
            if (value == null) {
                cache.put(key, new TimedEntry(null));
                return null;
            }
            String decodedValue = value.getDecodedValue();
            cache.put(key, new TimedEntry(decodedValue));
            return decodedValue;
        }
        return entry.getValue();
        
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Deprecated
    private String getConfigKey(String subKey){
        return KEY_PREFIX + subKey;
    }
    
    private Long getValidity() {
        if (validity == null) {
            validity = config.getOptionalValue(getKeyWithPrefix(KEY_VALIDITY), Long.class)
                .orElse(config.getOptionalValue(getConfigKey(KEY_VALIDITY), Long.class)// For backward compatibility with MicroProfile-ext
                .orElse(DEFAULT_VALIDITY) * 1000L); 
        }
        return validity;
    }

    private String getPrefix() {
        if (prefix == null) {
            prefix = config.getOptionalValue(getKeyWithPrefix(KEY_CONSUL_PREFIX), String.class).map(s -> s + "/")
                .orElse(config.getOptionalValue(getConfigKey(KEY_CONSUL_PREFIX), String.class).map(s -> s + "/")// For backward compatibility with MicroProfile-ext
                .orElse(DEFAULT_CONSUL_PREFIX)); 
        }
        return prefix;
    }

    private String getHost() {
        if (host == null) {
            host = config.getOptionalValue(getKeyWithPrefix(KEY_HOST), String.class)
                .orElse(config.getOptionalValue(getConfigKey(KEY_HOST), String.class)// For backward compatibility with MicroProfile-ext
                .orElse(DEFAULT_HOST)); 
        }
        return host;
    }
    
    private ConsulClient getClient() {
        if (client == null ) {
            log.info("Loading [consul] MicroProfile ConfigSource");
            client = new ConsulClient(substitutor.replace(getHost()));
        }
        return client;
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
            return (timestamp + getValidity()) < System.currentTimeMillis();
        }
    }

    private static final String KEY_PREFIX = "configsource.consul.";
    
    private static final String KEY_HOST = "host";
    private static final String DEFAULT_HOST = "localhost";
    
    private static final String KEY_VALIDITY = "validity";
    private static final Long DEFAULT_VALIDITY = 30L;

    private static final String KEY_CONSUL_PREFIX = "prefix";
    private static final String DEFAULT_CONSUL_PREFIX = "";
    
}
