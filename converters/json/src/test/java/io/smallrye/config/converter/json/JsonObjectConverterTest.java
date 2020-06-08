package io.smallrye.config.converter.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.config.inject.ConfigProducer;

/**
 * Testing the injection of a JsonObject
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ExtendWith(WeldJunit5Extension.class)
public class JsonObjectConverterTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(WeldInitiator.createWeld()
            .addExtensions(ConfigExtension.class)
            .addBeanClass(ConfigProducer.class)
            .addBeanClass(JsonObjectConverterTest.class))
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    @ConfigProperty(name = "someJsonObject")
    private JsonObject someValue;

    @Test
    public void testInjection() {
        try (JsonReader jsonReader = Json.createReader(new StringReader("{\"foo\": \"bar\", \"count\":100}"))) {
            JsonObject jsonObject = jsonReader.readObject();
            assertEquals(jsonObject, someValue);
        }
    }
}
