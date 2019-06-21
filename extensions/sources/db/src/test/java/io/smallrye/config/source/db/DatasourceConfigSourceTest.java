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
package io.smallrye.config.source.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasourceConfigSourceTest {

    private DatasourceConfigSource configSource;

    @BeforeEach
    public void init() {
        configSource = new DatasourceConfigSource();
        configSource.repository = mock(Repository.class);
    }

    @Test
    void testGetOrdinal() {
        assertTrue(configSource.getOrdinal() > 100);
    }

    @Test
    void testGetProperties_empty() {
        assertTrue(configSource.getProperties().isEmpty());
    }

    @Test
    public void testGetProperties_null() {
        when(configSource.repository.getConfigValue(anyString())).thenReturn(null);
        configSource.getValue("test");
        assertTrue(configSource.getProperties().isEmpty());
    }

    @Test
    public void testGetProperties_one() {
        when(configSource.repository.getAllConfigValues()).thenReturn(Collections.singletonMap("test", "value"));
        configSource.getValue("test");
        assertEquals(1, configSource.getProperties().size());
    }

    @Test
    public void testGetValue() {
        when(configSource.repository.getConfigValue(anyString())).thenReturn("123");
        assertEquals("123", configSource.getValue("test"));
    }
    
    @Test
    public void testGetValue_cache() {
        when(configSource.repository.getConfigValue(anyString())).thenReturn("123");
        configSource.getValue("test");
        configSource.getValue("test");
        verify(configSource.repository, times(1)).getConfigValue(anyString());
    }

}
