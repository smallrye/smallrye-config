/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
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

import java.util.Comparator;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ConfigSourceSortTestCase {

    static final Comparator<ConfigSource> CMP = SmallRyeConfig.CONFIG_SOURCE_COMPARATOR;

    static final class DummyConfigSource implements ConfigSource {
        private final int ordinal;

        DummyConfigSource(final int ordinal) {
            this.ordinal = ordinal;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public Map<String, String> getProperties() {
            return null;
        }

        public String getValue(final String propertyName) {
            return null;
        }

        public String getName() {
            return "placeholder";
        }
    }

    static ConfigSource src(int ordinal) {
        return new DummyConfigSource(ordinal);
    }

    @Test
    public void ensureCorrectOrdinalSort() {
        assertEquals(1, CMP.compare(src(100), src(200)));
        assertEquals(1, CMP.compare(src(-100), src(200)));
        assertEquals(0, CMP.compare(src(0), src(0)));
        assertEquals(0, CMP.compare(src(-100), src(-100)));
        assertEquals(1, CMP.compare(src(0), src(Integer.MAX_VALUE)));
        assertEquals(1, CMP.compare(src(Integer.MIN_VALUE), src(0)));
        assertEquals(-1, CMP.compare(src(0), src(Integer.MIN_VALUE)));
        assertEquals(-1, CMP.compare(src(Integer.MAX_VALUE), src(0)));
    }
}
