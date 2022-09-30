package io.smallrye.config.source.keystore;

import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "io.smallrye.config.source.keystore")
public interface KeyStoreConfig {
    @WithParentName
    Map<String, KeyStore> keystores();

    interface KeyStore {
        String path();

        @WithDefault("PKCS12")
        String type();

        String password();

        Optional<String> algorithm();

        Map<String, Alias> aliases();

        interface Alias {
            Optional<String> name();

            Optional<String> password();

            Optional<String> algorithm();
        }
    }
}
