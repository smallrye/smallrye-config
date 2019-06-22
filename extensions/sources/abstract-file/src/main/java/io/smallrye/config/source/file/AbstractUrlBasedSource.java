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
package io.smallrye.config.source.file;

import io.smallrye.config.events.ChangeEventNotifier;
import io.smallrye.config.source.EnabledConfigSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.Config;

/**
 * URL Based property files
 * 
 * Load some file from a file and convert to properties.
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
public abstract class AbstractUrlBasedSource extends EnabledConfigSource implements Reloadable {
    private final LinkedHashMap<URL,Map<String, String>> propertiesMap = new LinkedHashMap<>();
    private final Map<String, String> properties = new HashMap<>();
    private final String urlInputString;
    private final String keySeparator;
    private final boolean pollForChanges;
    private final int pollInterval;
    private final boolean notifyOnChanges;
    
    private FileResourceWatcher fileResourceWatcher = null;
    private WebResourceWatcher webResourceWatcher = null;
    
    private final Config config;
    
    public AbstractUrlBasedSource(){
        String ext = getFileExtension();
        log.log(Level.INFO, "Loading [{0}] MicroProfile ConfigSource", ext); // Only used for backward compatible with MicroProfile-ext
        this.config = super.getConfig();
        this.keySeparator = loadPropertyKeySeparator();
        this.notifyOnChanges = loadNotifyOnChanges();
        this.pollForChanges = loadPollForChanges();
        this.pollInterval = loadPollInterval();
        this.urlInputString = loadUrlPath();
        this.loadUrls(urlInputString);
        
        super.initOrdinal(500); 
    }
    
    
    @Override
    protected Map<String, String> getPropertiesIfEnabled() {
        return this.properties;
    }

    @Override
    public String getValue(String key) {
        // in case we are about to configure ourselves we simply ignore that key
        if(super.isEnabled() && !key.startsWith(getPrefix()) && !key.startsWith(getKeyWithPrefix(null))){
            return this.properties.get(key);
        }
        return null;
    }

    @Override
    public String getName() {
        return getClassKeyPrefix();
    }
    
    @Override
    public void reload(URL url){
        Map<String, String> before = new HashMap<>(this.properties);
        initialLoad(url);
        mergeProperties();
        Map<String, String> after = new HashMap<>(this.properties);
        if(notifyOnChanges)ChangeEventNotifier.getInstance().detectChangesAndFire(before, after,getName());
    }
    
    private void initialLoad(URL url){
        
        log.log(Level.INFO, "Using [{0}] as {1} URL", new Object[]{url.toString(), getFileExtension()});
        
        InputStream inputStream = null;
        
        try {
            inputStream = url.openStream();
            if (inputStream != null) {
                this.propertiesMap.put(url, toMap(inputStream));
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to read URL [{0}] - {1}", new Object[]{url, e.getMessage()});
        } finally {
            try {
                if (inputStream != null)inputStream.close();
            // no worries, means that the file is already closed
            } catch (IOException e) {}
        }
    }
    
    protected String getKeySeparator(){
        return this.keySeparator;
    }
    
    private void loadUrls(String surl) {
        String urls[] = surl.split(COMMA);
        
        for(String u:urls){
            if(u!=null && !u.isEmpty()){
                loadUrl(u.trim());
            }
        }
        mergeProperties();
    }
    
    private void loadUrl(String url) {
        try {
            URL u = new URL(url);
            initialLoad(u);
            
            if(shouldPollForChanges()){
                // Local (file://...)
                if(isLocalResource(u)){
                    enableLocalResourceWatching(u);
                // Web (http://...)
                }else if(isWebResource(u)){
                    enableWebResourceWatching(u);
                // TODO: Add support for other protocols ?     
                }else{
                    log.log(Level.WARNING, "Can not detect changes on resource {0}", u.getFile());
                }
            }
        } catch (MalformedURLException ex) {
            log.log(Level.WARNING, "Can not load URL [" + url + "]", ex);
        }
    }
    
    private void enableLocalResourceWatching(URL u){
        if(isLocalResource(u)){
            if(this.fileResourceWatcher==null)this.fileResourceWatcher = new FileResourceWatcher(this,pollInterval);
            this.fileResourceWatcher.startWatching(u);
        }
    }
    
    private void enableWebResourceWatching(URL u){
        if(isWebResource(u)){
            if(this.webResourceWatcher==null)this.webResourceWatcher = new WebResourceWatcher(this,pollInterval);
            this.webResourceWatcher.startWatching(u);
        }
    }
    
    private boolean isLocalResource(URL u){
        return u.getProtocol().equalsIgnoreCase(FILE);
    }
    
    private boolean isWebResource(URL u){
        return u.getProtocol().equalsIgnoreCase(HTTP) || u.getProtocol().equalsIgnoreCase(HTTPS);
    }
    
    private boolean shouldPollForChanges(){
        return this.pollForChanges && this.pollInterval>0;
    }
    
    private void mergeProperties(){
        this.properties.clear();
        Set<Map.Entry<URL, Map<String, String>>> entrySet = propertiesMap.entrySet();
        for(Map.Entry<URL, Map<String, String>> entry:entrySet){
            this.properties.putAll(entry.getValue());
        }
    }
    
    private String loadPropertyKeySeparator(){
        return config.getOptionalValue(getKeyWithPrefix(KEY_SEPARATOR), String.class)
            .orElse(config.getOptionalValue(getConfigKey(KEY_SEPARATOR), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(DOT));
    }
    
    private boolean loadPollForChanges(){
        return config.getOptionalValue(getKeyWithPrefix(POLL_FOR_CHANGES), Boolean.class)
            .orElse(config.getOptionalValue(getConfigKey(POLL_FOR_CHANGES), Boolean.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_POLL_FOR_CHANGES));
    }
    
    private boolean loadNotifyOnChanges(){
        return config.getOptionalValue(getKeyWithPrefix(NOTIFY_ON_CHANGES), Boolean.class)
            .orElse(config.getOptionalValue(getConfigKey(NOTIFY_ON_CHANGES), Boolean.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_NOTIFY_ON_CHANGES));    
    }
    
    private int loadPollInterval(){
        return config.getOptionalValue(getKeyWithPrefix(POLL_INTERVAL), Integer.class)
            .orElse(config.getOptionalValue(getConfigKey(POLL_INTERVAL), Integer.class)// For backward compatibility with MicroProfile-ext
            .orElse(DEFAULT_POLL_INTERVAL));    
    }
    
    private String loadUrlPath(){
        return config.getOptionalValue(getKeyWithPrefix(URL), String.class)
            .orElse(config.getOptionalValue(getConfigKey(URL), String.class)// For backward compatibility with MicroProfile-ext
            .orElse(getDefaultUrl()));    
    }
    
    @Deprecated
    private String getConfigKey(String subKey){
        return getPrefix() + subKey;
    }
    
    @Deprecated
    private String getPrefix(){
        return CONFIGSOURCE + DOT + getFileExtension() + DOT;
    }
    
    private String getDefaultUrl(){
        String path = APPLICATION + DOT + getFileExtension();
        try {
            URL u = Paths.get(path).toUri().toURL();
            return u.toString();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static final String COMMA = ",";
    private static final String URL = "url";
    private static final String KEY_SEPARATOR = "keyseparator";
    
    private static final String POLL_FOR_CHANGES = "pollForChanges";
    private static final boolean DEFAULT_POLL_FOR_CHANGES = false;
    
    private static final String NOTIFY_ON_CHANGES = "notifyOnChanges";
    private static final boolean DEFAULT_NOTIFY_ON_CHANGES = true;
    
    private static final String POLL_INTERVAL = "pollInterval";
    private static final int DEFAULT_POLL_INTERVAL = 5; // 5 seconds
    
    private static final String CONFIGSOURCE = "configsource";
    private static final String APPLICATION = "application";
    private static final String FILE = "file";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    
    protected abstract String getFileExtension();
    protected abstract Map<String,String> toMap(final InputStream inputStream);
}