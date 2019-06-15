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
package io.smallrye.ext.config.source.properties;

import io.smallrye.config.ConfigFactory;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.config.inject.ConfigProducer;
import java.io.File;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Ignore
@Log
@RunWith(Arquillian.class)
public class ListTest {

    @Inject
    @ConfigProperty(name = "listTest")
    String stringList; 
    
    @Inject
    @ConfigProperty(name = "listTest")
    List<String> listList;
    
    @Inject
    @ConfigProperty(name = "listTest")
    Set<String> setList;
    
    @Inject
    @ConfigProperty(name = "listTest")
    String[] arrayList; 
    
    @Inject
    @ConfigProperty(name = "deepList.level1")
    List<String> deepList;
    
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, ConfigFactory.class.getPackage())
                .addPackages(true, ConfigProducer.class.getPackage())
                .addAsServiceProviderAndClasses(Extension.class, ConfigExtension.class)
                .addAsServiceProviderAndClasses(ConfigSource.class, PropertiesConfigSource.class)
                .addAsResource(new File("src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"), "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsResource(new File("src/test/resources/META-INF/microprofile-config.properties"), "META-INF/microprofile-config.properties")
                .addAsManifestResource("META-INF/beans.xml");

        
    }

    @Test
    public void testStringList() {
        Assert.assertEquals("item1,item2,item3\\,stillItem3", stringList);
    }

    @Test
    public void testListList() {
        Assert.assertNotNull(listList);
        Assert.assertEquals(3, listList.size());
        
    }
    
    @Test
    public void testSetList() {
        Assert.assertNotNull(setList);
        Assert.assertEquals(3, setList.size());
    }
    
    @Test
    public void testArrayList() {
        Assert.assertNotNull(arrayList);
        Assert.assertEquals(3, arrayList.length);
    }
    
    @Test
    public void testDeepList(){
        Assert.assertNotNull(deepList);
        Assert.assertEquals(2, deepList.size());        
    }
}
