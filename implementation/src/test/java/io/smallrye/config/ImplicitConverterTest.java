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

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.Converters.Implicit.HyphenateEnumConverter;

class ImplicitConverterTest {
    @Test
    void implicitURLConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("my.url", "https://github.com/smallrye/smallrye-config/"))
                .build();
        URL url = config.getValue("my.url", URL.class);
        assertNotNull(url);
        assertEquals("https", url.getProtocol());
        assertEquals("github.com", url.getHost());
        assertEquals("/smallrye/smallrye-config/", url.getPath());
    }

    @Test
    void implicitLocalDateConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("my.date", "2019-04-01"))
                .build();
        LocalDate date = config.getValue("my.date", LocalDate.class);
        assertNotNull(date);
        assertEquals(2019, date.getYear());
        assertEquals(4, date.getMonthValue());
        assertEquals(1, date.getDayOfMonth());
    }

    @Test
    void serializationOfConstructorConverter() {
        Converter<File> converter = Converters.Implicit.getConverter(File.class);

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
        A_B,
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
        HyphenateEnumConverter<MyEnum> converter = new HyphenateEnumConverter<>(MyEnum.class);
        assertEquals(MyEnum.DISCARD, converter.convert("DISCARD"));
        assertEquals(MyEnum.DISCARD, converter.convert("discard"));
        assertEquals(MyEnum.READ_UNCOMMITTED, converter.convert("READ_UNCOMMITTED"));
        assertEquals(MyEnum.A_B, converter.convert("a-b"));
        assertEquals(MyEnum.READ_UNCOMMITTED, converter.convert("read-uncommitted"));
        assertEquals(MyEnum.SIGUSR1, converter.convert("SIGUSR1"));
        assertEquals(MyEnum.SIGUSR1, converter.convert("sigusr1"));
        assertEquals(MyEnum.TrendBreaker, converter.convert("TrendBreaker"));
        assertEquals(MyEnum.TrendBreaker, converter.convert("trend-breaker"));
        assertEquals(MyEnum.MAKING_LifeDifficult, converter.convert("MAKING_LifeDifficult"));
        assertEquals(MyEnum.MAKING_LifeDifficult, converter.convert("making-life-difficult"));
    }

    @Test
    public void convertMyOtherEnum() {
        HyphenateEnumConverter<MyOtherEnum> converter = new HyphenateEnumConverter<>(MyOtherEnum.class);
        assertEquals(MyOtherEnum.makingLifeDifficult, converter.convert("makingLifeDifficult"));
        assertEquals(MyOtherEnum.makingLifeDifficult, converter.convert("making-life-difficult"));
        assertEquals(MyOtherEnum.READ__UNCOMMITTED, converter.convert("READ__UNCOMMITTED"));
        assertEquals(MyOtherEnum.READ__UNCOMMITTED, converter.convert("read-uncommitted"));
    }

    @Test
    public void illegalEnumConfigUtilConversion() {
        HyphenateEnumConverter<MyEnum> hyphenateEnumConverter = new HyphenateEnumConverter<>(MyEnum.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> hyphenateEnumConverter.convert("READUNCOMMITTED"));
        assertEquals(
                "SRCFG00049: Cannot convert READUNCOMMITTED to enum class io.smallrye.config.ImplicitConverterTest$MyEnum, " +
                        "allowed values: trend-breaker,making-life-difficult,discard,sigusr1,read-uncommitted,a-b",
                exception.getMessage());
    }

    public enum KeyEncryptionAlgorithm {
        RSA_OAEP,
        RSA_OAEP_256,
        ECDH_ES,
        ECDH_ES_A128KW,
        ECDH_ES_A192KW,
        ECDH_ES_A256KW,
        A128KW,
        A192KW,
        A256KW,
        A128GCMKW,
        A192GCMKW,
        A256GCMKW,
        PBES2_HS256_A128KW,
        PBES2_HS384_A192KW,
        PBES2_HS512_A256KW;
    }

    @Test
    void convertKeyEncryptionAlgorithm() {
        HyphenateEnumConverter<KeyEncryptionAlgorithm> converter = new HyphenateEnumConverter<>(KeyEncryptionAlgorithm.class);
        assertEquals(KeyEncryptionAlgorithm.RSA_OAEP, converter.convert("RSA_OAEP"));
        assertEquals(KeyEncryptionAlgorithm.RSA_OAEP, converter.convert("RSA-OAEP"));
        assertEquals(KeyEncryptionAlgorithm.RSA_OAEP, converter.convert("rsa-oaep"));
        assertEquals(KeyEncryptionAlgorithm.RSA_OAEP_256, converter.convert("RSA_OAEP_256"));
        assertEquals(KeyEncryptionAlgorithm.RSA_OAEP_256, converter.convert("RSA-OAEP-256"));
        assertEquals(KeyEncryptionAlgorithm.RSA_OAEP_256, converter.convert("rsa-oaep-256"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES, converter.convert("ECDH_ES"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES, converter.convert("ECDH-ES"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES, converter.convert("ecdh-es"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A128KW, converter.convert("ECDH_ES_A128KW"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A128KW, converter.convert("ECDH-ES-A128KW"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A128KW, converter.convert("ecdh-es-a128kw"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A192KW, converter.convert("ECDH_ES_A192KW"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A192KW, converter.convert("ECDH-ES-A192KW"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A192KW, converter.convert("ecdh-es-a192kw"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A256KW, converter.convert("ECDH_ES_A256KW"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A256KW, converter.convert("ECDH-ES-A256KW"));
        assertEquals(KeyEncryptionAlgorithm.ECDH_ES_A256KW, converter.convert("ecdh-es-a256kw"));
        assertEquals(KeyEncryptionAlgorithm.A128KW, converter.convert("A128KW"));
        assertEquals(KeyEncryptionAlgorithm.A128KW, converter.convert("a128kw"));
        assertEquals(KeyEncryptionAlgorithm.A192KW, converter.convert("A192KW"));
        assertEquals(KeyEncryptionAlgorithm.A192KW, converter.convert("a192kw"));
        assertEquals(KeyEncryptionAlgorithm.A256KW, converter.convert("A256KW"));
        assertEquals(KeyEncryptionAlgorithm.A256KW, converter.convert("a256kw"));
        assertEquals(KeyEncryptionAlgorithm.A128GCMKW, converter.convert("A128GCMKW"));
        assertEquals(KeyEncryptionAlgorithm.A128GCMKW, converter.convert("a128gcmkw"));
        assertEquals(KeyEncryptionAlgorithm.A192GCMKW, converter.convert("A192GCMKW"));
        assertEquals(KeyEncryptionAlgorithm.A192GCMKW, converter.convert("a192gcmkw"));
        assertEquals(KeyEncryptionAlgorithm.A256GCMKW, converter.convert("A256GCMKW"));
        assertEquals(KeyEncryptionAlgorithm.A256GCMKW, converter.convert("a256gcmkw"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS256_A128KW, converter.convert("PBES2_HS256_A128KW"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS256_A128KW, converter.convert("PBES2-HS256-A128KW"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS256_A128KW, converter.convert("pbes2-hs256-a128kw"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS384_A192KW, converter.convert("PBES2_HS384_A192KW"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS384_A192KW, converter.convert("PBES2-HS384-A192KW"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS384_A192KW, converter.convert("pbes2-hs384-a192kw"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS512_A256KW, converter.convert("PBES2_HS512_A256KW"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS512_A256KW, converter.convert("PBES2-HS512-A256KW"));
        assertEquals(KeyEncryptionAlgorithm.PBES2_HS512_A256KW, converter.convert("pbes2-hs512-a256kw"));
    }

    enum HTTPConduit {
        QuarkusCXFDefault,
        CXFDefault,
        HttpClientHTTPConduitFactory,
        URLConnectionHTTPConduitFactory
    }

    @Test
    void convertHttpConduit() {
        HyphenateEnumConverter<HTTPConduit> converter = new HyphenateEnumConverter<>(HTTPConduit.class);
        assertEquals(HTTPConduit.QuarkusCXFDefault, converter.convert("QuarkusCXFDefault"));
        assertEquals(HTTPConduit.QuarkusCXFDefault, converter.convert("quarkus-cxf-default"));
        assertEquals(HTTPConduit.CXFDefault, converter.convert("CXFDefault"));
        assertEquals(HTTPConduit.CXFDefault, converter.convert("cxf-default"));
        assertEquals(HTTPConduit.HttpClientHTTPConduitFactory, converter.convert("HttpClientHTTPConduitFactory"));
        assertEquals(HTTPConduit.HttpClientHTTPConduitFactory, converter.convert("http-client-http-conduit-factory"));
        assertEquals(HTTPConduit.URLConnectionHTTPConduitFactory, converter.convert("URLConnectionHTTPConduitFactory"));
        assertEquals(HTTPConduit.URLConnectionHTTPConduitFactory, converter.convert("url-connection-http-conduit-factory"));
    }
}
