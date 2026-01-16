module io.smallrye.config.crypto {
    requires transitive io.smallrye.config;

    exports io.smallrye.config.crypto;

    provides io.smallrye.config.SecretKeysHandlerFactory with
        io.smallrye.config.crypto.AESGCMNoPaddingSecretKeysHandlerFactory;
}