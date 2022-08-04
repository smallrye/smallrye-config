package io.smallrye.config.source.filevault;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class FileVaultConfigSourceTest {
    @Test
    void secret() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .addDiscoveredInterceptors()
                .build();

        assertEquals("username", config.getRawValue("username"));
        assertEquals("username", config.getRawValue("expression"));

        ConfigValue secret = config.getConfigValue("secret");
        assertNotNull(secret, secret.getValue());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf
                .generateCertificate(new ByteArrayInputStream(secret.getValue().getBytes(ISO_8859_1)));
        assertTrue(cert.getSubjectX500Principal().getName().startsWith("CN=Quarkus,OU=Quarkus,O=Quarkus"));
        assertEquals("${file-vault:keystore/mykey}", secret.getRawValue());
    }
}
