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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.AbstractLocationConfigSourceFactory;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.source.keystore.KeyStoreConfig.KeyStore.Alias;

public class KeyStoreConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        KeyStoreConfig keyStoreConfig = getKeyStoreConfig(context);

        // A keystore may contain the encryption key for a handler, so we load keystore that do not have handlers
        Map<String, KeyStoreConfig.KeyStore> prioritized = new HashMap<>();
        Map<String, KeyStoreConfig.KeyStore> late = new HashMap<>();
        for (Map.Entry<String, KeyStoreConfig.KeyStore> keyStoreEntry : keyStoreConfig.keystores().entrySet()) {
            if (keyStoreEntry.getValue().handler().isEmpty()) {
                prioritized.put(keyStoreEntry.getKey(), keyStoreEntry.getValue());
            } else {
                late.put(keyStoreEntry.getKey(), keyStoreEntry.getValue());
            }
        }

        List<ConfigSource> keyStoreSources = new ArrayList<>();
        for (Map.Entry<String, KeyStoreConfig.KeyStore> keyStoreEntry : prioritized.entrySet()) {
            for (ConfigSource configSource : loadKeyStoreSources(context, keyStoreEntry.getKey(),
                    keyStoreEntry.getValue())) {
                keyStoreSources.add(configSource);
            }
        }

        ConfigSourceContext keyStoreContext = new ConfigSourceContext() {
            final SmallRyeConfig contextConfig = new SmallRyeConfigBuilder()
                    .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                    .withSources(keyStoreSources).build();

            @Override
            public ConfigValue getValue(final String name) {
                return contextConfig.getConfigValue(name);
            }

            @Override
            public Iterator<String> iterateNames() {
                return contextConfig.getPropertyNames().iterator();
            }
        };

        for (Map.Entry<String, KeyStoreConfig.KeyStore> keyStoreEntry : late.entrySet()) {
            for (ConfigSource configSource : loadKeyStoreSources(keyStoreContext, keyStoreEntry.getKey(),
                    keyStoreEntry.getValue())) {
                keyStoreSources.add(configSource);
            }
        }

        return keyStoreSources;
    }

    private static KeyStoreConfig getKeyStoreConfig(final ConfigSourceContext context) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withMapping(KeyStoreConfig.class)
                .withMappingIgnore("smallrye.config.source.keystore.*.password")
                .build();
        return config.getConfigMapping(KeyStoreConfig.class);
    }

    private static Iterable<ConfigSource> loadKeyStoreSources(final ConfigSourceContext context, final String name,
            final KeyStoreConfig.KeyStore keyStore) {
        return new AbstractLocationConfigSourceFactory() {
            @Override
            protected String[] getFileExtensions() {
                return new String[0];
            }

            @Override
            protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
                ConfigValue password = getPassword(context, name);
                return new UrlKeyStoreConfigSource(url, ordinal).loadKeyStore(keyStore, password.getValue().toCharArray());
            }

            @Override
            public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                return loadConfigSources(keyStore.path(), 100);
            }

        }.getConfigSources(context);
    }

    // Avoid caching the keystore password
    private static ConfigValue getPassword(final ConfigSourceContext context, final String name) {
        // TODO - name can be quoted. try to figure out a better way to do this
        String passwordName = "smallrye.config.source.keystore." + name + ".password";
        ConfigValue password = context.getValue(passwordName);
        if (password == null || password.getValue() == null) {
            passwordName = "smallrye.config.source.keystore.\"" + name + "\".password";
            password = context.getValue(passwordName);
            if (password == null || password.getValue() == null) {
                throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(passwordName));
            }
        }
        return password;
    }

    private static class UrlKeyStoreConfigSource implements ConfigSource {
        private final URL url;
        private final int ordinal;

        UrlKeyStoreConfigSource(final URL url, final int ordinal) {
            this.url = url;
            this.ordinal = ordinal;
        }

        ConfigSource loadKeyStore(KeyStoreConfig.KeyStore keyStoreConfig, char[] password) throws IOException {
            try {
                KeyStore keyStore = KeyStore.getInstance(keyStoreConfig.type());
                keyStore.load(url.openStream(), password);

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
                        public Optional<String> handler() {
                            return keyStoreConfig.handler();
                        }
                    });

                    if (keyStore.isKeyEntry(alias)) {
                        Key key = keyStore.getKey(alias, password);
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
