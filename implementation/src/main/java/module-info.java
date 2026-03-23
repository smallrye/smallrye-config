module io.smallrye.config {
    requires io.smallrye.common.classloader;
    requires io.smallrye.common.constraint;
    requires io.smallrye.common.expression;
    requires io.smallrye.common.function;

    requires io.smallrye.config.common;

    requires jakarta.annotation;

    requires transitive org.eclipse.microprofile.config;

    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;

    requires static org.objectweb.asm;

    requires jdk.unsupported;

    exports io.smallrye.config;
    exports io.smallrye.config._private to
        io.smallrye.config.inject,
        io.smallrye.config.source.file,
        io.smallrye.config.source.keystore,
        io.smallrye.config.crypto;

    uses io.smallrye.config.SmallRyeConfigFactory;
    uses io.smallrye.config.ConfigSourceFactory;
    uses io.smallrye.config.ConfigSourceInterceptor;
    uses io.smallrye.config.ConfigSourceInterceptorFactory;
    uses io.smallrye.config.ConfigValidator;
    uses io.smallrye.config.SecretKeysHandler;
    uses io.smallrye.config.SecretKeysHandlerFactory;
    uses io.smallrye.config.SmallRyeConfigBuilderCustomizer;

    uses org.eclipse.microprofile.config.spi.ConfigSource;
    uses org.eclipse.microprofile.config.spi.ConfigSourceProvider;
    uses org.eclipse.microprofile.config.spi.Converter;

    provides io.smallrye.config.ConfigSourceFactory with
        io.smallrye.config.PropertiesLocationConfigSourceFactory;
    provides org.eclipse.microprofile.config.spi.ConfigProviderResolver with
        io.smallrye.config.SmallRyeConfigProviderResolver;
}