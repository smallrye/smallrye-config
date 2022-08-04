package io.smallrye.config.source.jasypt;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "io.smallrye.config.jasypt")
public interface JasyptConfig {
    Encryptor encryptor();

    interface Encryptor {
        String password();

        String algorithm();
    }
}
