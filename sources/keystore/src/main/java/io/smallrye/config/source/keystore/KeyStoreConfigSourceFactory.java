package io.smallrye.config.source.keystore;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.AbstractLocationConfigSourceFactory;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory.ConfigurableConfigSourceFactory;
import io.smallrye.config.ConfigurableConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.source.keystore.KeyStoreConfig.KeyStore.Alias;

public class KeyStoreConfigSourceFactory implements ConfigurableConfigSourceFactory<KeyStoreConfig> {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context, KeyStoreConfig keyStoreConfig) {
        List<ConfigSource> keyStoreSources = new ArrayList<>();
        for (Map.Entry<String, KeyStoreConfig.KeyStore> keyStoreEntry : keyStoreConfig.keystores().entrySet()) {
            KeyStoreConfig.KeyStore keyStore = keyStoreEntry.getValue();
            keyStoreSources.add(new ConfigurableConfigSource(new AbstractLocationConfigSourceFactory() {
                @Override
                protected String[] getFileExtensions() {
                    return new String[0];
                }

                @Override
                protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
                    return new UrlKeyStoreConfigSource(url, ordinal).loadKeyStore(keyStore);
                }

                @Override
                public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                    return loadConfigSources(keyStore.path(), 100);
                }
            }));
        }

        return keyStoreSources;
    }

    private static class UrlKeyStoreConfigSource implements ConfigSource {
        private final URL url;
        private final int ordinal;

        UrlKeyStoreConfigSource(final URL url, final int ordinal) {
            this.url = url;
            this.ordinal = ordinal;
        }

        ConfigSource loadKeyStore(KeyStoreConfig.KeyStore keyStoreConfig) throws IOException {
            try {
                KeyStore keyStore = KeyStore.getInstance(keyStoreConfig.type());
                keyStore.load(url.openStream(), keyStoreConfig.password().toCharArray());

                Map<String, String> properties = new HashMap<>();
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Alias aliasConfig = keyStoreConfig.aliases().getOrDefault(alias, new Alias() {
                        @Override
                        public Optional<String> name() {
                            return Optional.of(alias);
                        }

                        @Override
                        public Optional<String> password() {
                            return Optional.of(keyStoreConfig.password());
                        }

                        @Override
                        public Optional<String> handler() {
                            return keyStoreConfig.handler();
                        }
                    });

                    if (keyStore.isKeyEntry(alias)) {
                        Key key = keyStore.getKey(alias,
                                aliasConfig.password().orElse(keyStoreConfig.password()).toCharArray());
                        String encoded;
                        Optional<String> handler = aliasConfig.handler();
                        if (handler.isPresent()) {
                            encoded = "${" + handler.get() + "::" + new String(key.getEncoded(), UTF_8) + "}";
                        } else {
                            encoded = new String(key.getEncoded(), UTF_8);
                        }
                        properties.put(aliasConfig.name().orElse(alias), encoded);
                    } else if (keyStore.isCertificateEntry(alias)) {
                        // TODO
                    }
                }
                return new PropertiesConfigSource(properties, this.getName(), this.getOrdinal());
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public Set<String> getPropertyNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getValue(final String propertyName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return "KeyStoreConfigSource[source=" + url.toString() + "]";
        }
    }
}
