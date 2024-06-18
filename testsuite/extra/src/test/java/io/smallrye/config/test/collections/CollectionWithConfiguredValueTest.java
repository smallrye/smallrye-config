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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test that injected collections (array, List, Set) uses configured values.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@ExtendWith(ArquillianExtension.class)
class CollectionWithConfiguredValueTest {

    @Deployment
    static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "CollectionWithConfiguredValueTest.jar")
                .addClasses(CollectionWithConfiguredValueTest.class, CollectionBean.class)
                .addAsManifestResource("beans.xml")
                .addAsManifestResource(new StringAsset("myPets=snake,ox"), "microprofile-config.properties")
                .as(JavaArchive.class);
        return ShrinkWrap
                .create(WebArchive.class, "CollectionWithConfiguredValueTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    CollectionBean bean;

    @Test
    void collectionWithConfiguredValues() {
        String[] arrayPets = bean.getArrayPets();
        assertNotNull(arrayPets);
        assertEquals(2, arrayPets.length);
        assertArrayEquals(new String[] { "snake", "ox" }, arrayPets);

        List<String> listPets = bean.getListPets();
        assertNotNull(listPets);
        assertEquals(2, listPets.size());
        assertIterableEquals(new ArrayList<>(Arrays.asList("snake", "ox")), listPets);

        Set<String> setPets = bean.getSetPets();
        assertNotNull(setPets);
        assertEquals(2, setPets.size());
        assertIterableEquals(new HashSet<>(Arrays.asList("snake", "ox")), setPets);
    }

    @Test
    void collectionWithMissingValues() {
        try {
            bean.getListWithEmptyDefault().get();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
        }

        try {
            bean.getListWithNoDefault().get();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
        }
    }
}
