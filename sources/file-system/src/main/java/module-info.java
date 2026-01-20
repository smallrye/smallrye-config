module io.smallrye.config.source.file {
    requires io.smallrye.config;
    requires io.smallrye.config.common;

    requires org.eclipse.microprofile.config;
    requires org.jboss.logging;
    requires org.jboss.logging.annotations;

    exports io.smallrye.config.source.file;

    provides io.smallrye.config.ConfigSourceFactory with
        io.smallrye.config.source.file.FileSystemConfigSourceFactory;
}