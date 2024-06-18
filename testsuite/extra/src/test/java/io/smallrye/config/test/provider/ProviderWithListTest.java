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
package io.smallrye.config.test.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test that configuration is injected into Provider.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@ExtendWith(ArquillianExtension.class)
class ProviderWithListTest {
    @Deployment
    static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ProviderTest.jar")
                .addClasses(ProviderWithListTest.class, Email.class, ProviderBeanWithList.class)
                .addAsManifestResource("beans.xml")
                .addAsManifestResource(new StringAsset("objectIds=a,b,c\nnumbers=4,5,6"), "microprofile-config.properties")
                .as(JavaArchive.class);
        return ShrinkWrap
                .create(WebArchive.class, "ProviderTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    ProviderBeanWithList bean;

    @Test
    void provider() {
        Provider<List<String>> provider = bean.objectdIds;
        assertNotNull(provider);
        List<String> stuff = provider.get();
        assertNotNull(stuff);
        assertEquals(3, stuff.size());
        assertEquals("a", stuff.get(0));
        assertEquals("b", stuff.get(1));
        assertEquals("c", stuff.get(2));

        Provider<List<Integer>> numberProvider = bean.numbers;
        assertNotNull(numberProvider);
        List<Integer> numbers = numberProvider.get();
        assertNotNull(numbers);
        assertEquals(3, numbers.size());
        assertEquals(4, (int) numbers.get(0));
    }
}
