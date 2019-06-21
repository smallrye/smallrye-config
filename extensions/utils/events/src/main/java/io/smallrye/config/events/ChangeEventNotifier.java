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
package io.smallrye.config.events;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import lombok.extern.java.Log;

/**
 * Easy way to fire a change event
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 * 
 * This gets used from Config sources that is not in the CDI Context. So we can not @Inject a bean.
 * For some reason, CDI.current() is only working on Payara, and not on Thorntail and OpenLiberty, so this ugly footwork is to
 * get around that.
 */
@Log
@ApplicationScoped
public class ChangeEventNotifier {

    @Inject
    private Event<ChangeEvent> broadcaster;
    
    private static ChangeEventNotifier INSTANCE;
    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        INSTANCE = this;
    }
 
    public static ChangeEventNotifier getInstance(){
        //return CDI.current().select(ChangeEventNotifier.class).get();
        return INSTANCE;
    }
    
    public void detectChangesAndFire(Map<String, String> before,Map<String, String> after, String fromSource){
        List<ChangeEvent> changes = new ArrayList<>();
        if(!before.equals(after)){
            Set<Map.Entry<String, String>> beforeEntries = before.entrySet();
            for(Map.Entry<String,String> beforeEntry:beforeEntries){
                String key = beforeEntry.getKey();
                String oldValue = beforeEntry.getValue();
                if(after.containsKey(key)){
                    String newValue = after.get(key);
                    if(!newValue.equals(oldValue)){
                        // Update
                        changes.add(new ChangeEvent(Type.UPDATE, key, getOptionalOldValue(oldValue), newValue, fromSource));
                    }
                    after.remove(key);
                }else{
                    // Removed.
                    changes.add(new ChangeEvent(Type.REMOVE, key, getOptionalOldValue(oldValue), null, fromSource));
                }
            }
            
            Set<Map.Entry<String, String>> newEntries = after.entrySet();
            for(Map.Entry<String,String> newEntry:newEntries){
                // New
                changes.add(new ChangeEvent(Type.NEW, newEntry.getKey(), Optional.empty(), newEntry.getValue(), fromSource));
            }
        }
        
        if(!changes.isEmpty())fire(changes);
    }
    
    public void fire(ChangeEvent changeEvent){
        List<Annotation> annotationList = new ArrayList<>();
        annotationList.add(new TypeFilter.TypeFilterLiteral(changeEvent.getType()));
        annotationList.add(new KeyFilter.KeyFilterLiteral(changeEvent.getKey()));
        annotationList.add(new SourceFilter.SourceFilterLiteral(changeEvent.getFromSource()));
        
        broadcaster.select(annotationList.toArray(new Annotation[annotationList.size()])).fire(changeEvent);
    }
    
    public void fire(List<ChangeEvent> changeEvents){
        for(ChangeEvent changeEvent : changeEvents){
            fire(changeEvent);
        }
    }
    
    public Optional<String> getOptionalOldValue(String oldValue){
        if(oldValue==null || oldValue.isEmpty())return Optional.empty();
        return Optional.of(oldValue);
    }
    
}
