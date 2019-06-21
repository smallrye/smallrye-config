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
package io.smallrye.config.source.properties;

import java.io.File;
import javax.inject.Inject;
import static org.assertj.core.api.Assertions.assertThat;
import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:dpmoore@acm.org">Derek P. Moore</a>
 */
@RunWith(Arquillian.class)
public class EnabledWhenEnabledKeyIsTrueTest {

    @Inject
    Config config;

    @Deployment
    public static WebArchive createDeployment() {
        final File[] thorntailMPConfigFiles = Maven.resolver().resolve("io.thorntail:microprofile-config:2.4.0.Final").withoutTransitivity().asFile();
        
        return ShrinkWrap.create(WebArchive.class, "PropertiesConfigSourceTest.war")
                .addPackage(PropertiesConfigSource.class.getPackage())
                .addAsLibraries(thorntailMPConfigFiles)
                .addAsResource(new File("src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"), "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsResource(EnabledWhenEnabledKeyIsTrueTest.class.getClassLoader().getResource("config-enabled.properties"), "META-INF/microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @Test
    public void testPropertyLoadsWhenExplicitlyEnabled() {
        assertThat(config.getOptionalValue("test.property", String.class)).get()
                .isEqualTo("a-string-value")
                .as("test.property in application.properties is set to a-string-value");
    }

}
