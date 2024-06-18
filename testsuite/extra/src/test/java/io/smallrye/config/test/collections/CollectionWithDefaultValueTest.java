/*
 * Copyright 2018 Red Hat, Inc.
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
package io.smallrye.config.test.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test that injected collections (array, List, Set) uses default values in the
 * absence of configured values.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@ExtendWith(ArquillianExtension.class)
class CollectionWithDefaultValueTest {

    @Deployment
    static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "CollectionWithDefaultValueTest.jar")
                .addClasses(CollectionWithDefaultValueTest.class, CollectionBean.class)
                .addAsManifestResource("beans.xml")
                .as(JavaArchive.class);
        return ShrinkWrap
                .create(WebArchive.class, "CollectionWithDefaultValueTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    CollectionBean bean;

    @Test
    void collectionWithDefaultValues() {
        String[] arrayPets = bean.getArrayPets();
        assertNotNull(arrayPets);
        assertEquals(2, arrayPets.length);
        assertArrayEquals(new String[] { "horse", "monkey" }, arrayPets);

        List<String> listPets = bean.getListPets();
        assertNotNull(listPets);
        assertEquals(2, listPets.size());
        assertIterableEquals(new ArrayList<>(Arrays.asList("cat", "lama")), listPets);

        Set<String> setPets = bean.getSetPets();
        assertNotNull(setPets);
        assertEquals(2, setPets.size());
        assertTrue(setPets.contains("dog"));
        assertTrue(setPets.contains("mouse"));
    }
}
