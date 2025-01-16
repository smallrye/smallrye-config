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

import io.smallrye.config._private.ConfigMessages;

/**
 * This {@link AbstractLocationConfigSourceLoader} loads {@link ConfigSource}s from a list of specific
 * locations.
 * <p>
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
 * If a profile is active, the profile resource is only loaded if the unprofiled resource is available in the exact
 * location. This is to keep a consistent loading order and match the unprofiled resource when looking for resources
 * in the classpath. The order is usually not guaranteed when querying a {@link ClassLoader} for multiple resources.
 * The implementation queries the {@link ClassLoader} for the profiled resources by requiring the unprofiled resource
 * first. It pairs them with unprofiled resources to ensure a consistent order and override behavior.
 * <p>
 * Profiles are not taken into account if the location is a directory.
 */
public abstract class AbstractLocationConfigSourceLoader {
    private static final Converter<URI> URI_CONVERTER = new URIConverter();

    /**
     * If the lookup from an {@link URL} which the scheme {@code file:} should fail. By default, a failed load does not
     * throw an exception. In situations where the resource is required, a return value of {@code true} enables the
     * exception.
     *
     * @return {@code true} if file lookup should fail with an exception, {@code false} otherwise.
     */
    protected boolean failOnMissingFile() {
        return false;
    }

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
     * @throws IOException if an error occurred when reading from the {@link URL}.
     */
    protected abstract ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException;

    protected List<ConfigSource> loadConfigSources(final String location, final int ordinal) {
        return loadConfigSources(location != null ? new String[] { location } : null, ordinal);
    }

    protected List<ConfigSource> loadConfigSources(final String location, final int ordinal, final ClassLoader classLoader) {
        return loadConfigSources(location != null ? new String[] { location } : null, ordinal, classLoader);
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
                    configSources.add(loadConfigSource(path.toUri(), ordinal));
                }
            } catch (IOException e) {
                throw ConfigMessages.msg.failedToLoadResource(e, uri.toString());
            }
        } else if ("file".equals(uri.getScheme()) && Files.notExists(urlPath) && failOnMissingFile()) {
            throw ConfigMessages.msg.failedToLoadResource(new FileNotFoundException(uri.toString()), uri.toString());
        }
        return configSources;
    }

    protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
        final List<ConfigSource> configSources = new ArrayList<>();
        final ClassLoader useClassloader = classLoader != null ? classLoader : SecuritySupport.getContextClassLoader();
        try {
            consumeAsPaths(useClassloader, uri.getPath(),
                    new ConfigSourceClassPathConsumer(classLoader, uri, ordinal, configSources));
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e, uri.toString());
        } catch (IllegalArgumentException e) {
            return fallbackToUnknownProtocol(uri, ordinal, useClassloader);
        }
        return configSources;
    }

    protected List<ConfigSource> tryJar(final URI uri, final int ordinal) {
        final List<ConfigSource> configSources = new ArrayList<>();
        try {
            consumeAsPath(toURL(uri), new ConfigSourcePathConsumer(ordinal, configSources));
        } catch (Exception e) {
            throw ConfigMessages.msg.failedToLoadResource(e, uri.toString());
        }
        return configSources;
    }

    protected List<ConfigSource> fallbackToUnknownProtocol(final URI uri, final int ordinal, final ClassLoader classLoader) {
        List<ConfigSource> configSources = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(uri.toString());
            while (resources.hasMoreElements()) {
                URL resourceUrl = resources.nextElement();
                if (validExtension(resourceUrl.getFile())) {
                    ConfigSource mainSource = loadConfigSourceInternal(resourceUrl, ordinal);
                    configSources.add(mainSource);
                    configSources.add(new ConfigurableConfigSource((ProfileConfigSourceFactory) profiles -> {
                        List<ConfigSource> profileSources = new ArrayList<>();
                        for (int i = profiles.size() - 1; i >= 0; i--) {
                            int mainOrdinal = mainSource.getOrdinal() + profiles.size() - i + 1;
                            for (String fileExtension : getFileExtensions()) {
                                URI profileUri = addProfileName(uri, profiles.get(i), fileExtension);
                                try {
                                    Enumeration<URL> profileResources = classLoader.getResources(profileUri.toString());
                                    while (profileResources.hasMoreElements()) {
                                        profileSources
                                                .addAll(loadProfileConfigSource(profileResources.nextElement(), mainOrdinal));
                                    }
                                } catch (IOException e) {
                                    // It is ok to not find the resource here, because it is an optional profile resource.
                                }
                            }
                        }
                        return profileSources;
                    }));
                }
            }
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e, uri.toString());
        }
        return configSources;
    }

    protected List<ConfigSource> tryHttpResource(final URI uri, final int ordinal) {
        final List<ConfigSource> configSources = new ArrayList<>();
        if (validExtension(uri.getPath())) {
            ConfigSource mainSource = loadConfigSource(uri, ordinal);
            configSources.add(mainSource);
            configSources.add(profileConfigSourceFactory(uri, mainSource.getOrdinal()));
        }
        return configSources;
    }

    private static URL toURL(final URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ConfigSource loadConfigSourceInternal(final URL url, int ordinal) {
        try {
            return loadConfigSource(url, ordinal);
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e, url.toString());
        }
    }

    private ConfigSource loadConfigSource(final URI uri, final int ordinal) {
        URL url = toURL(uri);
        return loadConfigSourceInternal(url, ordinal);
    }

    private List<ConfigSource> loadProfileConfigSource(final URL url, final int ordinal) {
        try {
            return List.of(loadConfigSource(url, ordinal));
        } catch (FileNotFoundException | NoSuchFileException e) {
            // It is ok to not find the resource here, because it is an optional profile resource.
            return Collections.emptyList();
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e, url.toString());
        }
    }

    private List<ConfigSource> loadProfileConfigSource(final URI uri, final int ordinal) {
        return loadProfileConfigSource(toURL(uri), ordinal);
    }

    private boolean validExtension(final Path fileName) {
        return validExtension(fileName.getFileName().toString());
    }

    private boolean validExtension(final String resourceName) {
        String[] fileExtensions = getFileExtensions();

        if (fileExtensions.length == 0) {
            return true;
        }

        for (String s : fileExtensions) {
            if (resourceName.endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    private static URI addProfileName(final URI uri, final String profile, final String fileExtension) {
        if ("jar".equals(uri.getScheme())) {
            URI rec = addProfileName(URI.create(uri.getRawSchemeSpecificPart()), profile, fileExtension);
            String recString = rec.toString();
            String finalString = new StringBuilder("jar:".length() + recString.length())
                    .append("jar:")
                    .append(recString)
                    .toString();
            return URI.create(finalString);
        }

        final String fileName = uri.getPath();
        assert fileName != null;

        final int dot = fileName.lastIndexOf(".");
        final String fileNameProfile;
        if (dot != -1 && dot != 0 && fileName.charAt(dot - 1) != '/') {
            String substring = fileName.substring(0, dot);
            fileNameProfile = new StringBuilder(substring.length() + 1 + profile.length() + 1 + fileExtension.length())
                    .append(substring)
                    .append("-")
                    .append(profile)
                    .append(".")
                    .append(fileExtension)
                    .toString();
        } else {
            fileNameProfile = new StringBuilder(fileName.length() + 1 + profile.length())
                    .append(fileName)
                    .append("-")
                    .append(profile)
                    .toString();
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

    private ConfigurableConfigSource profileConfigSourceFactory(final URI uri, final int ordinal) {
        return new ConfigurableConfigSource(new ConfigurableProfileConfigSourceFactory(uri, ordinal));
    }

    protected final class ConfigurableProfileConfigSourceFactory implements ProfileConfigSourceFactory {
        private final URI uri;
        private final int ordinal;

        public ConfigurableProfileConfigSourceFactory(final URI uri, final int ordinal) {
            this.uri = uri;
            this.ordinal = ordinal;
        }

        @Override
        public Iterable<ConfigSource> getProfileConfigSources(final List<String> profiles) {
            List<ConfigSource> profileSources = new ArrayList<>();
            for (int i = profiles.size() - 1; i >= 0; i--) {
                int ordinal = this.ordinal + profiles.size() - i;
                for (String fileExtension : getFileExtensions()) {
                    URI profileUri = addProfileName(uri, profiles.get(i), fileExtension);
                    profileSources.addAll(loadProfileConfigSource(profileUri, ordinal));
                }
            }
            return profileSources;
        }

        @Override
        public OptionalInt getPriority() {
            return OptionalInt.of(ordinal);
        }
    }

    protected final class ConfigSourcePathConsumer implements Consumer<Path> {
        private final int ordinal;
        private final List<ConfigSource> configSources;

        public ConfigSourcePathConsumer(final int ordinal, final List<ConfigSource> configSources) {
            this.ordinal = ordinal;
            this.configSources = configSources;
        }

        @Override
        public void accept(final Path path) {
            AbstractLocationConfigSourceLoader loader = AbstractLocationConfigSourceLoader.this;
            if (loader.validExtension(path.getFileName().toString())) {
                ConfigSource mainSource = loader.loadConfigSource(path.toUri(), ordinal);
                configSources.add(mainSource);
                configSources.add(profileConfigSourceFactory(path.toUri(), mainSource.getOrdinal()));
            }
        }
    }

    protected final class ConfigSourceClassPathConsumer implements Consumer<Path> {
        private final ClassLoader classLoader;
        private final URI resource;

        private final int ordinal;
        private final List<ConfigSource> configSources;

        public ConfigSourceClassPathConsumer(final ClassLoader classLoader, final URI resource, final int ordinal,
                final List<ConfigSource> configSources) {
            this.classLoader = classLoader;
            this.resource = resource;
            this.ordinal = ordinal;
            this.configSources = configSources;
        }

        @Override
        public void accept(final Path path) {
            AbstractLocationConfigSourceLoader loader = AbstractLocationConfigSourceLoader.this;
            if (loader.validExtension(path.getFileName().toString())) {
                ConfigSource mainSource = loader.loadConfigSource(path.toUri(), ordinal);
                configSources.add(mainSource);
                configSources.add(new ConfigurableConfigSource(new ProfileConfigSourceFactory() {
                    @Override
                    public Iterable<ConfigSource> getProfileConfigSources(final List<String> profiles) {
                        List<ConfigSource> profileSources = new ArrayList<>();
                        for (int i = profiles.size() - 1; i >= 0; i--) {
                            int ordinal = mainSource.getOrdinal() + profiles.size() - i;
                            for (String fileExtension : getFileExtensions()) {
                                URI profileResource = addProfileName(resource, profiles.get(i), fileExtension);
                                URI profileUri = addProfileName(path.toUri(), profiles.get(i), fileExtension);
                                if ("jar".equals(profileUri.getScheme()) || isInClassloader(profileResource, profileUri)) {
                                    profileSources.addAll(loader.loadProfileConfigSource(profileUri, ordinal));
                                }
                            }
                        }
                        return profileSources;
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(mainSource.getOrdinal());
                    }
                }));
            }
        }

        private boolean isInClassloader(final URI profileResource, final URI profileUri) {
            return classLoader.resources(profileResource.getPath())
                    .anyMatch(url -> url.toString().equals(profileUri.toString()));
        }
    }
}
