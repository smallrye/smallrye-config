package io.smallrye.config.source.file;

import static io.smallrye.config.Converters.newArrayConverter;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;

public class FileSystemConfigSourceFactory implements ConfigSourceFactory {
    public static final String SMALLRYE_CONFIG_SOURCE_FILE_LOCATIONS = "smallrye.config.source.file.locations";
    private static final String WILDCARD_LOCATION_SUFFIX = "/*/";

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue(SMALLRYE_CONFIG_SOURCE_FILE_LOCATIONS);
        if (value == null || value.getValue() == null) {
            return Collections.emptyList();
        }

        return Stream
                .of(newArrayConverter(Converters.getImplicitConverter(String.class), String[].class)
                            .convert(value.getValue()))
                .flatMap(location -> {
                             if (location.endsWith(WILDCARD_LOCATION_SUFFIX)) {
                                 final URI rootUrl = URI.create(location.substring(0, location.length() - WILDCARD_LOCATION_SUFFIX.length()));
                                 final String schema = rootUrl.getScheme();
                                 if (schema != null && !"file".equalsIgnoreCase(schema)) {
                                     throw new IllegalArgumentException("Wildcard source file location not supported for URL schema " + schema);
                                 }
                                 final File rootLocation = new File(rootUrl.getPath());
                                 if (rootLocation.isDirectory()) {
                                     return Stream.of(rootLocation.listFiles(pathname -> pathname.isDirectory()))
                                                  .map(subLocation -> new FileSystemConfigSource(subLocation));
                                 } else {
                                     return Stream.empty();
                                 }
                             } else {
                                 return Stream.of(new FileSystemConfigSource(location));
                             }
                         }
                )
                .collect(Collectors.toList());
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(290);
    }
}
