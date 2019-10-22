package io.smallrye.config.source.yaml;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing some list behavior
 * 
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
        return DeployableUnit.create();
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
    public void testDeepList() {
        Assert.assertNotNull(deepList);
        Assert.assertEquals(2, deepList.size());
    }
}
