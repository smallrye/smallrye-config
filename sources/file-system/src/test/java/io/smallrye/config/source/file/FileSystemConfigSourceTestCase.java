/*
 * Copyright 2017 Red Hat, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class FileSystemConfigSourceTestCase {
    @Test
    public void testConfigSourceFromDir() throws URISyntaxException {
        URL configDirURL = this.getClass().getResource("configDir");
        File dir = new File(configDirURL.toURI());

        ConfigSource configSource = new FileSystemConfigSource(dir);

        assertEquals(4567, configSource.getOrdinal());

        assertEquals("myValue1", configSource.getValue("myKey1"));
        assertEquals("true", configSource.getValue("myKey2"));
    }

    @Test
    public void testCharacterReplacement() throws URISyntaxException {
        URL configDirURL = this.getClass().getResource("configDir");
        File dir = new File(configDirURL.toURI());

        ConfigSource configSource = new FileSystemConfigSource(dir);
        // the non-alphanumeric chars may be replaced by _ 
        assertEquals("http://localhost:8080/my-service", configSource.getValue("MyService/mp-rest/url"));
        // or the file name is uppercased
        assertEquals("http://localhost:8080/other-service", configSource.getValue("OtherService/mp-rest/url"));
        // but the key is still case sensitive 
        assertNull(configSource.getValue("myservice/mp-rest/url"));
        // you can't rewrite the key, only the file name 
        assertNull(configSource.getValue("MYSERVICE_MP_REST_URL"));
    }
}
