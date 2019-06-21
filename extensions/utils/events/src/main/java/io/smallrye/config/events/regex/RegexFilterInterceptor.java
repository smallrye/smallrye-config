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
package io.smallrye.config.events.regex;

import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import io.smallrye.config.events.ChangeEvent;
import lombok.extern.java.Log;

@Log    
@RegexFilter(value = "")
@Interceptor
@Priority(100)
public class RegexFilterInterceptor {

    @AroundInvoke
    public Object observer(InvocationContext ctx) throws Exception {
        
        RegexFilter regexFilterAnnotation = ctx.getMethod().getAnnotation(RegexFilter.class);
        Field onField = regexFilterAnnotation.onField();
        String regex = regexFilterAnnotation.value();
        
        Optional<ChangeEvent> posibleChangeEvent = getChangeEvent(ctx);
        
        if(posibleChangeEvent.isPresent()){
            ChangeEvent changeEvent = posibleChangeEvent.get();            
            String value = getValueToApplyRegexOn(changeEvent,onField);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(value);
            boolean b = matcher.matches();  
            if(!b)return null;
        }else{
            log.log(Level.WARNING, "Can not find ChangeEvent parameter for method {0}. @RegexFilter is being ignored", ctx.getMethod().getName());
        }
        return ctx.proceed();
    }
    
    private String getValueToApplyRegexOn(ChangeEvent changeEvent,Field onField){
        String value = null;
        switch (onField){
            case key: 
                value = changeEvent.getKey();
                break;
            case fromSource:
                value = changeEvent.getFromSource();
                break;
            case newValue:
                value = changeEvent.getNewValue();
                break;
            case oldValue:
                value = changeEvent.getOldValue().orElse("");
        }
       
        return value;
    }
    
    private Optional<ChangeEvent> getChangeEvent(InvocationContext ctx){
        Object[] parameters = ctx.getParameters();
        
        for(Object parameter:parameters){
            if(parameter.getClass().equals(ChangeEvent.class)){
                ChangeEvent changeEvent = (ChangeEvent)parameter;
                return Optional.of(changeEvent);
            }
        }
        return Optional.empty();
    }   
}