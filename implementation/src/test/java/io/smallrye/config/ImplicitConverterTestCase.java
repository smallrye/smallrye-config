package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.time.LocalDate;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

public class ImplicitConverterTestCase {

    @Test
    public void testImplicitURLConverter() {
        Config config = buildConfig(
                "my.url", "https://github.com/smallrye/smallrye-config/");
        URL url = config.getValue("my.url", URL.class);
        assertNotNull(url);
        assertEquals("https", url.getProtocol());
        assertEquals("github.com", url.getHost());
        assertEquals("/smallrye/smallrye-config/", url.getPath());
    }

    @Test
    public void testImplicitLocalDateConverter() {
        Config config = buildConfig(
                "my.date", "2019-04-01");
        LocalDate date = config.getValue("my.date", LocalDate.class);
        assertNotNull(date);
        assertEquals(2019, date.getYear());
        assertEquals(4, date.getMonthValue());
        assertEquals(1, date.getDayOfMonth());
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

    @Test
    public void testSerializationOfConstructorConverter() {
        Converter<File> converter = ImplicitConverters.getConverter(File.class);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream)) {
            out.writeObject(converter);
        } catch (IOException ex) {
            fail("Constructor converter should be serializable, but could not serialize it: " + ex);
        }
        Object readObject = null;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            readObject = in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            fail("Constructor converter should be serializable, but could not deserialize a previously serialized instance: "
                    + ex);
        }
        assertEquals(converter.convert("/bad/path").getPath(),
                ((File) ((Converter) readObject).convert("/bad/path")).getPath(),
                "Converted values to have same file path");
    }
}
