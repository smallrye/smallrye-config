package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class ConfigSerializationTest {
    @Test
    public void serialize() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withInterceptors(new RelocateConfigSourceInterceptor((Serializable & Function<String, String>) name -> name))
                .withInterceptors(new FallbackConfigSourceInterceptor((Serializable & Function<String, String>) name -> name))
                .withSources(ConfigValueConfigSourceWrapper.wrap(KeyValuesConfigSource.config("my.prop", "1")))
                .build();

        assertEquals("1", config.getConfigValue("my.prop").getValue());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream)) {
            out.writeObject(config);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Config should be serializable, but could not serialize it");
        }

        Object readObject = null;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            readObject = in.readObject();
        } catch (Exception e) {
            fail("Config config should be serializable, but could not deserialize the instance");
        }

        SmallRyeConfig serialized = (SmallRyeConfig) readObject;
        assertEquals("1", serialized.getConfigValue("my.prop").getValue());
    }
}
