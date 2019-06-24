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
package io.smallrye.config.source.yaml;

import io.smallrye.config.source.file.AbstractUrlBasedSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.yaml.snakeyaml.Yaml;

/**
 * Yaml config source
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class YamlConfigSource extends AbstractUrlBasedSource {

    @Override
    protected String getFileExtension() {
        return "yaml";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, String> toMap(InputStream inputStream) {
        final Map<String,String> properties = new TreeMap<>();
        Yaml yaml = new Yaml();
        Map<String, Object> yamlInput = yaml.loadAs(inputStream, TreeMap.class);
        
        for (String key : yamlInput.keySet()) {
            populateMap(properties,key, yamlInput.get(key));
        }
        return properties;
    }
    
    @SuppressWarnings("unchecked")
    private void populateMap(Map<String,String> properties, String key, Object o) {
        if (o instanceof Map) {
            Map map = (Map)o;
            for (Object mapKey : map.keySet()) {
                populateEntry(properties, key,mapKey.toString(),map);
            }
        } else if (o instanceof List) {
            List<String> l = toStringList((List)o);
            properties.put(key,String.join(COMMA, l));
        } else{
            if(o!=null)properties.put(key,o.toString());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void populateEntry(Map<String,String> properties, String key, String mapKey, Map<String, Object> map){
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
    
    private static final String COMMA = ",";
}