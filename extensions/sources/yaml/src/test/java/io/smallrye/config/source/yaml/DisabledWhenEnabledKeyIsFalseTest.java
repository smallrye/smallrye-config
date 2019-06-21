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
package io.smallrye.config.source.yaml;

import java.io.File;
import java.util.NoSuchElementException;
import javax.inject.Inject;
import lombok.extern.java.Log;
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
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
@RunWith(Arquillian.class)
public class DisabledWhenEnabledKeyIsFalseTest {

    @Inject
    Config config;

    @Deployment
    public static WebArchive createDeployment() {
        
        final File[] thorntailMPConfigFiles = Maven.resolver().resolve("io.thorntail:microprofile-config:2.4.0.Final").withoutTransitivity().asFile();
        
        return ShrinkWrap.create(WebArchive.class, "YamlConfigSourceTest.war")
                .addPackage(YamlConfigSource.class.getPackage())
                .addAsLibraries(thorntailMPConfigFiles)
                .addAsResource(new File("src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"), "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsResource(DisabledWhenEnabledKeyIsFalseTest.class.getClassLoader().getResource("config-disabled.properties"), "META-INF/microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @Test(expected = NoSuchElementException.class)
    public void testPropertyFailsWhenExplicitlyDisabled() {
        config.getValue("test.property", String.class);
    }

}
