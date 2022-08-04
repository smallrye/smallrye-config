package io.smallrye.config.source.filevault;

import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "io.smallrye.config.file-vault")
public interface FileVaultConfig {
    @WithParentName
    Map<String, KeyStore> providers();

    interface KeyStore {
        @WithDefault("passwords.p12")
        String path();

        String secret();

        Optional<String> encryptionKey();
    }
}
