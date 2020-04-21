package io.smallrye.config.converter.json;

import java.io.StringReader;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.config.inject.ConfigProducer;

/**
 * Testing the injection of a JsonArray
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class JsonArrayConverterTest {
    @Rule
    public WeldInitiator weld = WeldInitiator.from(WeldInitiator.createWeld()
            .addExtensions(ConfigExtension.class)
            .addBeanClass(ConfigProducer.class)
            .addBeanClass(JsonArrayConverterTest.class))
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    @ConfigProperty(name = "someJsonArray")
    private JsonArray someValue;

    @Test
    public void testInjection() {
        try (JsonReader jsonReader = Json.createReader(new StringReader("[\"value1\",\"value2\",\"value3\"]"))) {
            JsonArray jsonArray = jsonReader.readArray();
            Assert.assertEquals(jsonArray, someValue);
        }
    }
}
