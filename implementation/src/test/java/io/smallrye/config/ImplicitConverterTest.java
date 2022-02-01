package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import io.smallrye.config.ImplicitConverters.HyphenateEnumConverter;

class ImplicitConverterTest {
    @Test
    void implicitURLConverter() {
        Config config = buildConfig(
                "my.url", "https://github.com/smallrye/smallrye-config/");
        URL url = config.getValue("my.url", URL.class);
        assertNotNull(url);
        assertEquals("https", url.getProtocol());
        assertEquals("github.com", url.getHost());
        assertEquals("/smallrye/smallrye-config/", url.getPath());
    }

    @Test
    void implicitLocalDateConverter() {
        Config config = buildConfig(
                "my.date", "2019-04-01");
        LocalDate date = config.getValue("my.date", LocalDate.class);
        assertNotNull(date);
        assertEquals(2019, date.getYear());
        assertEquals(4, date.getMonthValue());
        assertEquals(1, date.getDayOfMonth());
    }

    @Test
    void serializationOfConstructorConverter() {
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

    enum MyEnum {
        DISCARD,
        READ_UNCOMMITTED,
        SIGUSR1,
        TrendBreaker,
        MAKING_LifeDifficult,
    }

    enum MyOtherEnum {
        makingLifeDifficult,
        READ__UNCOMMITTED
    }

    @Test
    public void convertMyEnum() {
        HyphenateEnumConverter<MyEnum> hyphenateEnumConverter = new HyphenateEnumConverter<>(MyEnum.class);
        assertEquals(hyphenateEnumConverter.convert("DISCARD"), MyEnum.DISCARD);
        assertEquals(hyphenateEnumConverter.convert("discard"), MyEnum.DISCARD);
        assertEquals(hyphenateEnumConverter.convert("READ_UNCOMMITTED"), MyEnum.READ_UNCOMMITTED);
        assertEquals(hyphenateEnumConverter.convert("read-uncommitted"), MyEnum.READ_UNCOMMITTED);
        assertEquals(hyphenateEnumConverter.convert("SIGUSR1"), MyEnum.SIGUSR1);
        assertEquals(hyphenateEnumConverter.convert("sigusr1"), MyEnum.SIGUSR1);
        assertEquals(hyphenateEnumConverter.convert("TrendBreaker"), MyEnum.TrendBreaker);
        assertEquals(hyphenateEnumConverter.convert("trend-breaker"), MyEnum.TrendBreaker);
        assertEquals(hyphenateEnumConverter.convert("MAKING_LifeDifficult"), MyEnum.MAKING_LifeDifficult);
        //assertEquals(hyphenateEnumConverter.convert("making-life-difficult"), MyEnum.MAKING_LifeDifficult);
    }

    @Test
    public void convertMyOtherEnum() {
        HyphenateEnumConverter<MyOtherEnum> hyphenateEnumConverter = new HyphenateEnumConverter<>(MyOtherEnum.class);
        assertEquals(hyphenateEnumConverter.convert("makingLifeDifficult"), MyOtherEnum.makingLifeDifficult);
        assertEquals(hyphenateEnumConverter.convert("making-life-difficult"), MyOtherEnum.makingLifeDifficult);
        assertEquals(hyphenateEnumConverter.convert("READ__UNCOMMITTED"), MyOtherEnum.READ__UNCOMMITTED);
        //assertEquals(hyphenateEnumConverter.convert("read-uncommitted"), MyOtherEnum.READ__UNCOMMITTED);
    }

    @Test
    public void testIllegalEnumConfigUtilConversion() {
        HyphenateEnumConverter<MyEnum> hyphenateEnumConverter = new HyphenateEnumConverter<>(MyEnum.class);
        assertThrows(IllegalArgumentException.class, () -> hyphenateEnumConverter.convert("READUNCOMMITTED"));
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }
}
