package io.smallrye.config.jasypt;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.properties.PropertyValueEncryptionUtils;

import io.smallrye.config.SecretKeysHandler;

public class JasyptSecretKeysHandler implements SecretKeysHandler {
    @Override
    public String decode(final String secret) {
        // TODO - We need to be able to configure this in the Handler.
        // Option to configure it in the constructor or retrieve config on the fly?
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("jasypt");
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.initialize();
        return PropertyValueEncryptionUtils.decrypt(secret, encryptor);
    }

    @Override
    public String getName() {
        return "jasypt";
    }
}
