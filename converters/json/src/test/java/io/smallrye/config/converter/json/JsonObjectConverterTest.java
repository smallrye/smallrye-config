package io.smallrye.config.converter.json;

import java.io.File;
import java.io.StringReader;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
 * Testing the injection of a JsonObject
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@RunWith(Arquillian.class)
public class JsonObjectConverterTest {

    @Inject
    @ConfigProperty(name = "someJsonObject")
    private JsonObject someValue;

    @Deployment
    public static WebArchive createDeployment() {
        final File[] smallryeConfig = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("io.smallrye.config:smallrye-config")
                .withoutTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class, "JsonObjectConverterTest.war")
                .addPackage(JsonObjectConverter.class.getPackage())
                .addAsLibraries(smallryeConfig)
                .addAsResource(
                        new File("src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter"),
                        "META-INF/services/org.eclipse.microprofile.config.spi.Converter")
                .addAsResource(JsonObjectConverterTest.class.getClassLoader().getResource("test.properties"),
                        "META-INF/microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testInjection() {
        try (JsonReader jsonReader = Json.createReader(new StringReader("{\"foo\": \"bar\", \"count\":100}"))) {
            JsonObject jsonObject = jsonReader.readObject();
            Assert.assertEquals(jsonObject, someValue);
        }
    }
}
