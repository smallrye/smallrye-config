package io.smallrye.config.events;

import java.io.File;
import java.util.Optional;

import javax.enterprise.event.Observes;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.config.events.regex.RegexFilter;
import io.smallrye.config.events.regex.RegexFilterInterceptor;

/**
 * Testing that the events fire correctly
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@RunWith(Arquillian.class)
public class ChangeEventNotifierTest {

    @Deployment
    public static WebArchive createDeployment() {
        final File[] smallryeConfig = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("io.smallrye.config:smallrye-config")
                .withoutTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class, "ChangeEventNotifierTest.war")
                .addPackage(ChangeEventNotifier.class.getPackage())
                .addPackage(RegexFilterInterceptor.class.getPackage())
                .addAsLibraries(smallryeConfig)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testNewType() {
        ChangeEvent changeEvent = new ChangeEvent(Type.NEW, "test.key", Optional.empty(), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    public void testUpdateType() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "test.key", Optional.of("old value"), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    public void testRemoveType() {
        ChangeEvent changeEvent = new ChangeEvent(Type.REMOVE, "test.key", Optional.of("old value"), null, "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    public void testCertainKey() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "some.key", Optional.of("old value"), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    public void testCertainKeyAndUpdate() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "some.key", Optional.of("old value"), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    public void testCertainSource() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "some.key", Optional.of("old value"), "test value",
                "SomeConfigSource");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    public void testRegex() {
        ChangeEvent changeEvent = new ChangeEvent(Type.NEW, "testcase.key", Optional.empty(), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    public void listenForNew(@Observes @TypeFilter(Type.NEW) ChangeEvent changeEvent) {
        Assert.assertEquals("Expecting new type", Type.NEW, changeEvent.getType());
    }

    public void listenForUpdate(@Observes @TypeFilter(Type.UPDATE) ChangeEvent changeEvent) {
        Assert.assertEquals("Expecting update type", Type.UPDATE, changeEvent.getType());
    }

    public void listenForRemove(@Observes @TypeFilter(Type.REMOVE) ChangeEvent changeEvent) {
        Assert.assertEquals("Expecting remove type", Type.REMOVE, changeEvent.getType());
    }

    public void listenForCertainKey(@Observes @KeyFilter("some.key") ChangeEvent changeEvent) {
        Assert.assertEquals("Expecting certain key", "some.key", changeEvent.getKey());
    }

    public void listenForCertainKeyAndUpdate(
            @Observes @TypeFilter(Type.UPDATE) @KeyFilter("some.key") ChangeEvent changeEvent) {
        Assert.assertEquals("Expecting certain key", "some.key", changeEvent.getKey());
        Assert.assertEquals("Expecting update type", Type.UPDATE, changeEvent.getType());
    }

    public void listenForCertainSource(@Observes @SourceFilter("SomeConfigSource") ChangeEvent changeEvent) {
        Assert.assertEquals("Expecting certain config source", "SomeConfigSource", changeEvent.getFromSource());
    }

    @RegexFilter("^testcase\\..+")
    public void listenForKeyPattern(@Observes ChangeEvent changeEvent) {
        Assert.assertTrue("Expecting key to start with certain value", changeEvent.getKey().startsWith("testcase"));
    }
}
