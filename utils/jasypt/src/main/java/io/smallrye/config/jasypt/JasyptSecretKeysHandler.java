package io.smallrye.config.jasypt;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.properties.PropertyValueEncryptionUtils;

import io.smallrye.config.SecretKeysHandler;

public class JasyptSecretKeysHandler implements SecretKeysHandler {
    private final StandardPBEStringEncryptor encryptor;

    public JasyptSecretKeysHandler(final String password, final String algorithm) {
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        encryptor.setAlgorithm(algorithm);
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.initialize();
    }

    @Override
    public String decode(final String secret) {
        return PropertyValueEncryptionUtils.decrypt(secret, encryptor);
    }

    @Override
    public String getName() {
        return "jasypt";
    }
}
