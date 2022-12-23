package io.smallrye.config;

import static io.smallrye.common.classloader.ClassPathUtils.consumeAsPath;
import static io.smallrye.common.classloader.ClassPathUtils.consumeAsPaths;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * This {@link AbstractLocationConfigSourceLoader} loads {@link ConfigSource}s from a list of specific
 * locations.
 * <p>
 *
 * The locations comprise a list of valid {@link URI}s which are loaded in order. The following URI schemes are
 * supported:
 *
 * <ol>
 * <li>file or directory</li>
 * <li>classpath resource</li>
 * <li>jar resource</li>
 * <li>http resource</li>
 * </ol>
 * <p>
 *
 * If a profile is active, the profile resource is only loaded if the unprofiled resource is available in the same
 * location. This is to keep a consistent loading order and match with the unprofiled resource. Profiles are not
 * taken into account if the location is a directory.
 */
public abstract class AbstractLocationConfigSourceLoader {
    private static final Converter<URI> URI_CONVERTER = new URIConverter();

    /**
     * The file extensions to filter the locations to load. It does not require to include the dot separator.
     *
     * @return an array with the file extensions.
     */
    protected abstract String[] getFileExtensions();

    /**
     * Loads a {@link ConfigSource} from an {@link URL}. Implementations must construct the {@link ConfigSource} to
     * load.
     *
     * @param url the {@link URL} to load the {@link ConfigSource}.
     * @param ordinal the ordinal of the {@link ConfigSource}.
     *
     * @return the loaded {@link ConfigSource}.
     * @throws IOException if an error occurred when reading from the the {@link URL}.
     */
    protected abstract ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException;

    protected List<ConfigSource> loadConfigSources(final String location, final int ordinal) {
        return loadConfigSources(new String[] { location }, ordinal);
    }

    protected List<ConfigSource> loadConfigSources(final String location, final int ordinal, final ClassLoader classLoader) {
        return loadConfigSources(new String[] { location }, ordinal, classLoader);
    }

    protected List<ConfigSource> loadConfigSources(final String[] locations, final int ordinal) {
        return loadConfigSources(locations, ordinal, SecuritySupport.getContextClassLoader());
    }

    protected List<ConfigSource> loadConfigSources(final String[] locations, final int ordinal, final ClassLoader classLoader) {
        if (locations == null || locations.length == 0) {
            return Collections.emptyList();
        }

        final List<ConfigSource> configSources = new ArrayList<>();
        for (String location : locations) {
            final URI uri = URI_CONVERTER.convert(location);
            if (uri.getScheme() == null) {
                configSources.addAll(tryFileSystem(uri, ordinal));
                configSources.addAll(tryClassPath(uri, ordinal, classLoader));
            } else if (uri.getScheme().equals("file")) {
                configSources.addAll(tryFileSystem(uri, ordinal));
            } else if (uri.getScheme().equals("jar")) {
                configSources.addAll(tryJar(uri, ordinal));
            } else if (uri.getScheme().startsWith("http")) {
                configSources.addAll(tryHttpResource(uri, ordinal));
            } else {
                throw ConfigMessages.msg.schemeNotSupported(uri.getScheme());
            }
        }
        return configSources;
    }

    protected List<ConfigSource> tryFileSystem(final URI uri, final int ordinal) {
        final List<ConfigSource> configSources = new ArrayList<>();
        final Path urlPath = uri.getScheme() != null ? Paths.get(uri) : Paths.get(uri.getPath());
        if (Files.isRegularFile(urlPath)) {
            consumeAsPath(toURL(urlPath.toUri()), new ConfigSourcePathConsumer(ordinal, configSources));
        } else if (Files.isDirectory(urlPath)) {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(urlPath, this::validExtension)) {
                for (Path path : paths) {
                    addConfigSource(path.toUri(), ordinal, configSources);
                }
            } catch (IOException e) {
                throw ConfigMessages.msg.failedToLoadResource(e);
            }
        }
        return configSources;
    }

    protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
        final List<ConfigSource> configSources = new ArrayList<>();
        final ClassLoader useClassloader = classLoader != null ? classLoader : SecuritySupport.getContextClassLoader();
        try {
            consumeAsPaths(useClassloader, uri.getPath(), new ConfigSourcePathConsumer(ordinal, configSources));
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        } catch (IllegalArgumentException e) {
            configSources.addAll(fallbackToUnknownProtocol(uri, ordinal, useClassloader));
        }
        return configSources;
    }

    protected List<ConfigSource> tryJar(final URI uri, final int ordinal) {
        final List<ConfigSource> configSources = new ArrayList<>();
        try {
            consumeAsPath(toURL(uri), new ConfigSourcePathConsumer(ordinal, configSources));
        } catch (Exception e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        }
        return configSources;
    }

    protected List<ConfigSource> fallbackToUnknownProtocol(final URI uri, final int ordinal, final ClassLoader classLoader) {
        final List<ConfigSource> configSources = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(uri.toString());
            while (resources.hasMoreElements()) {
                final URL resourceUrl = resources.nextElement();
                if (validExtension(resourceUrl.getFile())) {
                    final ConfigSource mainSource = addConfigSource(resourceUrl, ordinal, configSources);
                    configSources.add(new ConfigurableConfigSource((ProfileConfigSourceFactory) profiles -> {
                        final List<ConfigSource> profileSources = new ArrayList<>();
                        for (int i = profiles.size() - 1; i >= 0; i--) {
                            final int mainOrdinal = mainSource.getOrdinal() + profiles.size() - i + 1;
                            final URI profileUri = addProfileName(uri, profiles.get(i));
                            try {
                                final Enumeration<URL> profileResources = classLoader.getResources(profileUri.toString());
                                while (profileResources.hasMoreElements()) {
                                    final URL profileUrl = profileResources.nextElement();
                                    addProfileConfigSource(profileUrl, mainOrdinal, profileSources);
                                }
                            } catch (IOException e) {
                                // It is ok to not find the resource here, because it is an optional profile resource.
                            }
                        }
                        return profileSources;
                    }));
                }
            }
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        }
        return configSources;
    }

    protected List<ConfigSource> tryHttpResource(final URI uri, final int ordinal) {
        final List<ConfigSource> configSources = new ArrayList<>();
        if (validExtension(uri.getPath())) {
            final ConfigSource mainSource = addConfigSource(uri, ordinal, configSources);
            configSources.addAll(tryProfiles(uri, mainSource));
        }
        return configSources;
    }

    protected List<ConfigSource> tryProfiles(final URI uri, final ConfigSource mainSource) {
        final List<ConfigSource> configSources = new ArrayList<>();
        configSources.add(new ConfigurableConfigSource(new ProfileConfigSourceFactory() {
            @Override
            public Iterable<ConfigSource> getProfileConfigSources(final List<String> profiles) {
                final List<ConfigSource> profileSources = new ArrayList<>();
                for (int i = profiles.size() - 1; i >= 0; i--) {
                    final int ordinal = mainSource.getOrdinal() + profiles.size() - i;
                    final URI profileUri = addProfileName(uri, profiles.get(i));
                    AbstractLocationConfigSourceLoader.this.addProfileConfigSource(toURL(profileUri), ordinal, profileSources);
                }
                return profileSources;
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(mainSource.getOrdinal());
            }
        }));
        return configSources;
    }

    private static URL toURL(final URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ConfigSource addConfigSource(final URI uri, final int ordinal, final List<ConfigSource> configSources) {
        return addConfigSource(toURL(uri), ordinal, configSources);
    }

    private ConfigSource addConfigSource(final URL url, final int ordinal, final List<ConfigSource> configSources) {
        try {
            ConfigSource configSource = loadConfigSource(url, ordinal);
            configSources.add(configSource);
            return configSource;
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        }
    }

    private void addProfileConfigSource(final URL profileToFileName, final int ordinal,
            final List<ConfigSource> profileSources) {
        try {
            profileSources.add(loadConfigSource(profileToFileName, ordinal));
        } catch (FileNotFoundException | NoSuchFileException e) {
            // It is ok to not find the resource here, because it is an optional profile resource.
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        }
    }

    private boolean validExtension(final Path fileName) {
        return validExtension(fileName.getFileName().toString());
    }

    private boolean validExtension(final String resourceName) {
        for (String s : getFileExtensions()) {
            if (resourceName.endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    private static URI addProfileName(final URI uri, final String profile) {
        if ("jar".equals(uri.getScheme())) {
            return URI.create("jar:" + addProfileName(URI.create(decodeIfNeeded(uri).getRawSchemeSpecificPart()), profile));
        }

        final String fileName = uri.getPath();
        assert fileName != null;

        final int dot = fileName.lastIndexOf(".");
        final String fileNameProfile;
        if (dot != -1 && dot != 0 && fileName.charAt(dot - 1) != '/') {
            fileNameProfile = fileName.substring(0, dot) + "-" + profile + fileName.substring(dot);
        } else {
            fileNameProfile = fileName + "-" + profile;
        }

        try {
            return new URI(uri.getScheme(),
                    uri.getAuthority(),
                    uri.getHost(),
                    uri.getPort(),
                    fileNameProfile,
                    uri.getQuery(),
                    uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class URIConverter implements Converter<URI> {
        private static final long serialVersionUID = -4852082279190307320L;

        @Override
        public URI convert(final String value) {
            try {
                return new URI(value);
            } catch (URISyntaxException e) {
                throw ConfigMessages.msg.uriSyntaxInvalid(e, value);
            }
        }
    }

    private class ConfigSourcePathConsumer implements Consumer<Path> {
        private final List<ConfigSource> configSources;
        private final int ordinal;

        public ConfigSourcePathConsumer(final int ordinal, final List<ConfigSource> configSources) {
            this.ordinal = ordinal;
            this.configSources = configSources;
        }

        @Override
        public void accept(final Path path) {
            final AbstractLocationConfigSourceLoader loader = AbstractLocationConfigSourceLoader.this;
            if (loader.validExtension(path.getFileName().toString())) {
                final ConfigSource mainSource = loader.addConfigSource(decodeIfNeeded(path.toUri()), ordinal, configSources);
                configSources.addAll(loader.tryProfiles(path.toUri(), mainSource));
            }
        }
    }

    // https://bugs.openjdk.java.net/browse/JDK-8131067 - For Java 8
    @Deprecated
    private static URI decodeIfNeeded(final URI uri) {
        if (uri.getScheme().equals("jar")) {
            return URI.create(uri.getScheme() + ":" + uri.getSchemeSpecificPart());
        } else {
            return uri;
        }
    }
}
