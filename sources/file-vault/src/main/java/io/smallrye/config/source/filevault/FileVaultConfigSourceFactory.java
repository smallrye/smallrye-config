package io.smallrye.config.source.filevault;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class FileVaultConfigSourceFactory implements ConfigSourceFactory {
    private static final Logger LOGGER = Logger.getLogger(FileVaultConfigSourceFactory.class.getName());

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ContextConfigSource(context))
                .withMapping(FileVaultConfig.class)
                .build();

        FileVaultConfig fileVaultConfig = config.getConfigMapping(FileVaultConfig.class);

        Map<String, String> properties = new HashMap<>();
        for (Map.Entry<String, FileVaultConfig.KeyStore> entry : fileVaultConfig.providers().entrySet()) {
            for (Map.Entry<String, String> keyStoreEntry : readKeyStore(entry.getValue()).entrySet()) {
                properties.put(
                        "io.smallrye.config.file-vault." + entry.getKey() + ".properties." + keyStoreEntry.getKey(),
                        keyStoreEntry.getValue());
            }
        }

        return Collections.singleton(new PropertiesConfigSource(properties, "FileVaultConfigSource", 100));
    }

    private static Map<String, String> readKeyStore(FileVaultConfig.KeyStore keyStore) {
        String keyStoreSecret = keyStore.encryptionKey().map(new Function<String, String>() {
            @Override
            public String apply(final String encryptionKey) {
                String decodedEncryptionKey = new String(Base64.getUrlDecoder().decode(encryptionKey), UTF_8);
                return EncryptionUtil.decrypt(keyStore.secret(), decodedEncryptionKey);
            }
        }).orElse(keyStore.secret());

        // TODO - Improve this
        URL keyStoreUrl = Thread.currentThread().getContextClassLoader().getResource(keyStore.path());
        if (keyStoreUrl != null) {
            return readKeyStore(keyStoreUrl, keyStoreSecret);
        } else {
            Path filePath = Paths.get(keyStore.path());
            if (Files.exists(filePath)) {
                try {
                    return readKeyStore(filePath.toUri().toURL(), keyStoreSecret);
                } catch (MalformedURLException e) {
                    LOGGER.errorf("Keystore %s location is not a valid URL", keyStore.path());
                    throw new RuntimeException(e);
                }
            } else {
                LOGGER.errorf("Keystore %s can not be found on the classpath and the file system", keyStore.path());
                throw new RuntimeException();
            }
        }
    }

    private static Map<String, String> readKeyStore(URL keyStoreFileUrl, String secret) {
        try (InputStream fis = keyStoreFileUrl.openStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fis, secret.toCharArray());

            Map<String, String> properties = new HashMap<>();
            for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                String storeEntry = loadStoreEntry(keyStore, secret, alias);
                if (storeEntry != null) {
                    properties.put(alias, storeEntry);
                }
            }

            return properties;
        } catch (IOException e) {
            LOGGER.errorf("Keystore %s can not be loaded", keyStoreFileUrl.toString());
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOGGER.errorf("Keystore %s entries can not be loaded", keyStoreFileUrl.toString());
            throw new RuntimeException(e);
        }
    }

    private static String loadStoreEntry(KeyStore keyStore, String secret, String alias) throws Exception {
        KeyStore.Entry entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(secret.toCharArray()));
        if (entry instanceof KeyStore.SecretKeyEntry) {
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            return new String(secretKey.getEncoded(), UTF_8);
        } else if (entry instanceof KeyStore.PrivateKeyEntry) {
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            if (certChain != null && certChain.length > 0) {
                return new String(certChain[0].getEncoded(), ISO_8859_1);
            }
        } else if (entry instanceof KeyStore.TrustedCertificateEntry) {
            return new String(((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate().getEncoded(), ISO_8859_1);
        }
        return null;
    }

    private static class ContextConfigSource implements ConfigSource {
        private final ConfigSourceContext context;

        public ContextConfigSource(final ConfigSourceContext context) {
            this.context = context;
        }

        @Override
        public Set<String> getPropertyNames() {
            Set<String> names = new HashSet<>();
            Iterator<String> namesIterator = context.iterateNames();
            while (namesIterator.hasNext()) {
                names.add(namesIterator.next());
            }
            return names;
        }

        @Override
        public String getValue(final String propertyName) {
            ConfigValue value = context.getValue(propertyName);
            return value != null && value.getValue() != null ? value.getValue() : null;
        }

        @Override
        public String getName() {
            return ContextConfigSource.class.getName();
        }
    }
}
