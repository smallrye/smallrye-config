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
 */
@ExtendWith(ArquillianExtension.class)
class ProviderAloneTest {
    @Deployment
    static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ProviderTest.jar")
                .addClasses(ProviderAloneTest.class, Email.class, ProviderAlone.class)
                .addAsManifestResource("beans.xml")
                .addAsManifestResource(new StringAsset("myEmail=example@smallrye.io"), "microprofile-config.properties")
                .as(JavaArchive.class);
        return ShrinkWrap
                .create(WebArchive.class, "ProviderTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    ProviderAlone bean;

    @Test
    void provider() {
        Provider<Email> emailProvider = bean.emailProvider;
        assertNotNull(emailProvider);
        Email email = emailProvider.get();
        assertNotNull(email);
        assertEquals("example", email.getName());
        assertEquals("smallrye.io", email.getDomain());
    }
}
