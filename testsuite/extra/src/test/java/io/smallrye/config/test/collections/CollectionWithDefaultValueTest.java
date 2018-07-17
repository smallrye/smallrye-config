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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test that injected collections (array, List, Set) uses default values in the
 * absence of configured values.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class CollectionWithDefaultValueTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "CollectionTest.jar")
                .addClasses(CollectionWithDefaultValueTest.class, CollectionBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "CollectionTest.war")
                .addAsLibrary(testJar);
        return war;
    }

    @Inject
    private CollectionBean bean;

    @Test
    public void testCollectionWithDefaultValues() {
        String[] arrayPets = bean.getArrayPets();
        assertNotNull(arrayPets);
        assertEquals(arrayPets.length, 2);
        assertEquals(arrayPets, new String[]{"horse", "monkey"});

        List<String> listPets = bean.getListPets();
        assertNotNull(listPets);
        assertEquals(listPets.size(), 2);
        assertEquals(listPets, new ArrayList<>(Arrays.asList("cat", "lama")));

        Set<String> setPets = bean.getSetPets();
        assertNotNull(setPets);
        assertEquals(setPets.size(), 2);
        assertEquals(setPets, new HashSet<>(Arrays.asList("dog", "mouse")));
    }
}
