/*
 * Copyright 2020 Red Hat, Inc.
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
package io.smallrye.config.test.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test that a high-priority converter for {@code Integer} will take precedence to built-in converters
 * for both Integer and int types.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2020 Red Hat inc.
 */
@ExtendWith(ArquillianExtension.class)
class ConverterTest {
    @Deployment
    static WebArchive deploy() {
        return ShrinkWrap
                .create(WebArchive.class, "CollectionWithConfiguredValueTest.war")
                .addClasses(ConverterTest.class, ConverterBean.class)
                .addClass(IntConverter.class)
                .addAsServiceProvider(Converter.class, IntConverter.class)
                .addAsWebInfResource("beans.xml");
    }

    @Inject
    ConverterBean bean;

    @Test
    void highPriorityConverterforInt() {
        assertEquals(102, bean.getInt());
    }

    @Test
    void highPriorityConverterforInteger() {
        assertEquals(102, bean.getInteger().intValue());
    }
}
