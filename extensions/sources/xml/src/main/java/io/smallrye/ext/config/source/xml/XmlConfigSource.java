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
package io.smallrye.ext.config.source.xml;

import io.smallrye.ext.config.source.filebase.AbstractUrlBasedSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.Config;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Xml config source
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
public class XmlConfigSource extends AbstractUrlBasedSource {
    
    @Override
    protected String getFileExtension() {
        return "xml";
    }
    
    @Override
    protected Map<String,String> toMap(final InputStream inputStream){
        try {
            InputSource inputSource = new InputSource(inputStream);
            return parse(inputSource);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            log.log(Level.WARNING, "Could not create properties from XML [{0}]", ex.getMessage());
            return new HashMap<>();
        }
    }
    
    private Map<String, String> parse(InputSource inputSource) throws SAXException, IOException, ParserConfigurationException {
        final Handler handler = new Handler(getConfig(),super.getKeySeparator());
        SAXParserFactory.newInstance().newSAXParser().parse(inputSource, handler);
        return handler.result;
    }

    private class Handler extends DefaultHandler {
        private final StringBuilder valuebuffer = new StringBuilder();
        private final List<String> keybuffer = new LinkedList<>();
        private final Map<String, String> result = new HashMap<>();
        
        private final boolean ignoreRoot;
        private final String keySeparator;
        private int depth = -1;
        
        public Handler(Config cfg,String keySeparator){
            this.ignoreRoot = cfg.getOptionalValue("io.smallrye.ext.config.source.xml.ignoreRoot", Boolean.class).orElse(true);
            this.keySeparator = keySeparator;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            depth++;
            if (this.depth == 0 && ignoreRoot) {
                // Ignoring root
            }else{
                keybuffer.add(qName);
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            final String value = valuebuffer.toString().trim();
            
            String key = String.join(keySeparator, keybuffer);
            if (!value.isEmpty()){
                if(result.containsKey(key)){
                    result.put(key, addToList(result.get(key),value));
                }else{
                    result.put(key, value.trim());
                }
            }
            valuebuffer.setLength(0);
            keybuffer.remove(qName);
            depth--;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            valuebuffer.append(ch, start, length);
        }
    }

    private String addToList(String existing,String newElement){
        if(newElement.contains(COMMA))newElement = newElement.replaceAll(COMMA, "\\\\,"); // Escape comma
        
        String[] split = existing.split(COMMA);
        List<String> l = new ArrayList<>(Arrays.asList(split));
        l.add(newElement);
        
        String join = String.join(COMMA, l);
        return join;
    }
    
    private static final String COMMA = ",";
}