/*
 * Copyright 2017 Red Hat, Inc.
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

package io.smallrye.config;

import javax.enterprise.inject.spi.Extension;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.smallrye.config.inject.ConfigExtension;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfigAuxiliaryArchiveAppender implements AuxiliaryArchiveAppender {

    @Override
    public Archive<?> createAuxiliaryArchive() {
        JavaArchive configJar = ShrinkWrap
                .create(JavaArchive.class, "smallrye-config.jar")
                .addPackage(SmallRyeConfig.class.getPackage())
                .addPackage(ConfigExtension.class.getPackage())
                .addAsServiceProvider(Extension.class, ConfigExtension.class)
                .addAsServiceProvider(ConfigProviderResolver.class, SmallRyeConfigProviderResolver.class);
        return configJar;
    }
}