/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

/**
 */
public class BuilderReuseTestCase {

    @Test
    public void testBuilderReuse() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.addDefaultSources();
        final Config config1 = builder.build();
        final Config config2 = builder.build();
        final Iterable<ConfigSource> cs1 = config1.getConfigSources();
        final Iterable<ConfigSource> cs2 = config2.getConfigSources();
        final Iterator<ConfigSource> it1 = cs1.iterator();
        final Iterator<ConfigSource> it2 = cs2.iterator();
        assertTrue(it1.hasNext() && it2.hasNext());
        assertEquals(it1.next().getClass(), it2.next().getClass());
        assertTrue(it1.hasNext() && it2.hasNext());
        assertEquals(it1.next().getClass(), it2.next().getClass());
        assertFalse(it1.hasNext() || it2.hasNext());
    }
}
