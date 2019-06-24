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
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing some list behavior
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@RunWith(Arquillian.class)
public class ListTest {

    @Inject
    @ConfigProperty(name = "listTest", defaultValue = "")
    String stringList; 
    
    @Inject
    @ConfigProperty(name = "listTest", defaultValue = "")
    List<String> listList;
    
    @Inject
    @ConfigProperty(name = "listTest", defaultValue = "")
    Set<String> setList;
    
    @Inject
    @ConfigProperty(name = "listTest", defaultValue = "")
    String[] arrayList; 
    
    @Inject
    @ConfigProperty(name = "deepList.level1", defaultValue = "")
    List<String> deepList;
    
    @Deployment
    public static WebArchive createDeployment() {
        final File[] thorntailMPConfigFiles = Maven.resolver().resolve("io.thorntail:microprofile-config:2.4.0.Final").withoutTransitivity().asFile();
        
        return ShrinkWrap.create(WebArchive.class, "YamlConfigSourceTest.war")
                .addPackage(YamlConfigSource.class.getPackage())
                .addAsLibraries(thorntailMPConfigFiles)
                .addAsResource(new File("src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"), "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
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
