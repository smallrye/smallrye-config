module io.smallrye.config.source.keystore {
    requires transitive io.smallrye.config;

    exports io.smallrye.config.source.keystore;

    // for the config mapping
    opens io.smallrye.config.source.keystore to
        io.smallrye.config;

    provides io.smallrye.config.ConfigSourceFactory with
        io.smallrye.config.source.keystore.KeyStoreConfigSourceFactory;
}