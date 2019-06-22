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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lombok.extern.java.Log;

/**
 * Watching files for changes
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
public class FileResourceWatcher {

    private final long pollInterval;
    private final Reloadable reloadable;
    
    public FileResourceWatcher(Reloadable reloadable, long pollInterval){
        this.reloadable = reloadable;
        this.pollInterval = pollInterval;
    }
    
    public void startWatching(URL url){
        try {
            Path path = Paths.get(url.toURI());
            Path dir = path.getParent();
            String filename = path.getFileName().toString();
            startWatching(dir,filename);
        } catch (URISyntaxException ex) {
            log.log(Level.WARNING, "Can not watch url [" + url +"]", ex);
        }
    }
    
    private void startWatching(Path path,String filter){
        if(FILTER_MAP.containsKey(path)){
            // Already watching this directory
            if(!FILTER_MAP.get(path).contains(filter))FILTER_MAP.get(path).add(filter);
        }else {
            // New folder to monitor
            List<String> filters = new ArrayList<>();
            filters.add(filter);
            FILTER_MAP.put(path, filters);
            
            ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
            try {
                WatchService ws = getWatcherService();
                WatchKey key = path.register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
                DIRECTORY_WATCHERS.put(key, path);
                // Here start Runable
                scheduledThreadPool.schedule(new Poller(ws),this.pollInterval,TimeUnit.SECONDS);
                
            } catch (IOException ex) {
                log.log(Level.WARNING, "Could not register directory [{0}] to watch for changes - {1}", new Object[]{path, ex.getMessage()});
            } finally {
                scheduledThreadPool.shutdown();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
    
    private WatchService getWatcherService() throws IOException{
        if(this.watcher!=null)return this.watcher;
        
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
            return this.watcher;
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Could not start watcher service [{0}]", ex.getMessage());
            throw ex;
        }
        
    }
    
    class Poller implements Runnable{
        final WatchService ws;
        Poller(WatchService ws){
            this.ws = ws;
        }
        
        @Override
        public void run() {
            WatchKey key;
            try {
                key = ws.take();
            } catch (InterruptedException x) {
                return;
            }

            Path d = DIRECTORY_WATCHERS.get(key);
            if (d != null) {

                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW)continue;
                    
                    WatchEvent<Path> ev = cast(event);
                    Path name = ev.context();
                    List<String> filters = FILTER_MAP.get(d);

                    if(filters.contains(name.toString())){
                        Path child = d.resolve(name);
                        try {
                            reloadable.reload(child.toUri().toURL());
                        } catch (MalformedURLException ex) {
                            log.log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            
            boolean valid = key.reset();
            if (!valid)DIRECTORY_WATCHERS.remove(key);
            
            this.run();
        }
    }
    
    private static final Map<WatchKey,Path> DIRECTORY_WATCHERS = new ConcurrentHashMap<>();
    private static final Map<Path,List<String>> FILTER_MAP = new ConcurrentHashMap<>();
    private WatchService watcher = null;
}