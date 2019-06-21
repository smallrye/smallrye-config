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
package io.smallrye.config.source.json;

import io.smallrye.config.source.filebase.AbstractUrlBasedSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import lombok.extern.java.Log;

/**
 * Json config source
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
public class JsonConfigSource extends AbstractUrlBasedSource {

    @Override
    protected String getFileExtension() {
        return "json";
    }

    @Override
    protected Map<String, String> toMap(final InputStream inputStream) {
        final Map<String,String> properties = new TreeMap<>();
                
        try (JsonReader reader = Json.createReader(inputStream)) {
            Map<String, Object> jsonMap = jsonToMap(reader.readObject());
            
            for (String key : jsonMap.keySet()) {
                populateMap(properties,key, jsonMap.get(key));
            }
        }
        
        return properties;
    }
    
    @SuppressWarnings("unchecked")
    private void populateMap(Map<String,String> properties,String key, Object o) {    
        if (o instanceof Map) {
            Map map = (Map)o;
            for (Object mapKey : map.keySet()) {
                populateEntry(properties,key,mapKey.toString(),map);
            }
        } else if (o instanceof List) {    
            List<String> l = toStringList((List)o);
            properties.put(key,String.join(COMMA, l));
        } else{
            if(o!=null)properties.put(key,o.toString());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void populateEntry(Map<String,String> properties,String key, String mapKey, Map<String, Object> map){
        String format = "%s" + super.getKeySeparator() + "%s";
        if (map.get(mapKey) instanceof Map) {
            populateMap(properties, String.format(format, key, mapKey), (Map<String, Object>) map.get(mapKey));
        } else if (map.get(mapKey) instanceof List) {
            List<String> l = toStringList((List)map.get(mapKey));
            properties.put(String.format(format, key, mapKey),String.join(COMMA, l));
        } else {
            properties.put(String.format(format, key, mapKey), map.get(mapKey).toString());
        }   
    }
    
    private List<String> toStringList(List l){
        List<String> nl = new ArrayList<>();
        for(Object o:l){
            String s = String.valueOf(o);
            if(s.contains(COMMA))s = s.replaceAll(COMMA, "\\\\,"); // Escape comma
            nl.add(s);
        }
        return nl;
    }
    
    private Map<String, Object> jsonToMap(JsonObject json) {
        Map<String, Object> retMap = new HashMap<>();

        if(json != JsonObject.NULL) {
            retMap = jsonObjectToMap(json);
        }
        return retMap;
    }

    private Object toObject(String key,JsonObject object){
        JsonValue.ValueType valueType = object.getValueType();
            
        if(valueType.equals(JsonValue.ValueType.ARRAY)) {
            return toList(object.getJsonArray(key));
        }else if (valueType.equals(JsonValue.ValueType.OBJECT)) {
            JsonValue jsonValue = object.get(key);
            JsonValue.ValueType objectType = jsonValue.getValueType();

            if(objectType.equals(JsonValue.ValueType.ARRAY)) {
                return toList(object.getJsonArray(key));
            }else if(objectType.equals(JsonValue.ValueType.STRING)){
                return object.getString(key);
            }else if(objectType.equals(JsonValue.ValueType.NUMBER)){
                return object.getInt(key);
            }else if(objectType.equals(JsonValue.ValueType.FALSE)){
                return false;
            }else if(objectType.equals(JsonValue.ValueType.TRUE)){
                return true;
            }else if(objectType.equals(JsonValue.ValueType.NULL)){
                return null;
            }else if(objectType.equals(JsonValue.ValueType.OBJECT)) {
                return jsonObjectToMap(object.getJsonObject(key));
            }else{
                return object.get(key);
            }
        }else{
            return object.get(key);
        }
    }
    
    private List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for(int i = 0; i < array.size(); i++) {
            
            
            Object value = array.get(i);
            if(value instanceof JsonArray) {
                value = toList((JsonArray) value);
            }else if(value instanceof JsonObject) {
                value = jsonObjectToMap((JsonObject) value);
            }else{
                JsonValue jv = array.get(i);
                JsonValue.ValueType objectType = jv.getValueType();
                
                if(objectType.equals(JsonValue.ValueType.STRING)){
                    value = array.getString(i);
                }else if(objectType.equals(JsonValue.ValueType.NUMBER)){
                    value = array.getInt(i);
                }else if(objectType.equals(JsonValue.ValueType.FALSE)){
                    value = false;
                }else if(objectType.equals(JsonValue.ValueType.TRUE)){
                    value = true;
                }else if(objectType.equals(JsonValue.ValueType.NULL)){
                    value = null;
                }
            }
            list.add(value);
        }
        return list;
    }
    
    private Map<String, Object> jsonObjectToMap(JsonObject object) throws JsonException {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keySet().iterator();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = toObject(key,object);
            map.put(key, value);
        }
        return map;
    }
    
    private static final String COMMA = ",";
}