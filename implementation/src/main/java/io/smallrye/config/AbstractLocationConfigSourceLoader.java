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
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.annotation.Experimental;

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
 * location. This is to keep a consistent loading order and matching with the unprofiled resource. Profiles are not
 * taken into account if the location is a directory.
 */
@Experimental("Loads sources by location")
public abstract class AbstractLocationConfigSourceLoader {
    private static final Converter<URI> URI_CONVERTER = new URIConverter();

    protected abstract String[] getFileExtensions();

    protected abstract ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException;

    protected ConfigSource loadConfigSource(final URL url) throws IOException {
        return this.loadConfigSource(url, ConfigSource.DEFAULT_ORDINAL);
    }

    protected List<ConfigSource> loadConfigSources(final String location) {
        return loadConfigSources(new String[] { location });
    }

    protected List<ConfigSource> loadConfigSources(final String location, final ClassLoader classLoader) {
        return loadConfigSources(new String[] { location }, classLoader);
    }

    protected List<ConfigSource> loadConfigSources(final String[] locations) {
        return loadConfigSources(locations, SecuritySupport.getContextClassLoader());
    }

    protected List<ConfigSource> loadConfigSources(final String[] locations, final ClassLoader classLoader) {
        if (locations == null || locations.length == 0) {
            return Collections.emptyList();
        }

        final List<ConfigSource> configSources = new ArrayList<>();
        for (String location : locations) {
            final URI uri = URI_CONVERTER.convert(location);
            if (uri.getScheme() == null || uri.getScheme().equals("file")) {
                configSources.addAll(tryFileSystem(uri));
                configSources.addAll(tryClassPath(uri, classLoader));
            } else if (uri.getScheme().equals("jar")) {
                configSources.addAll(tryJar(uri));
            } else if (uri.getScheme().startsWith("http")) {
                configSources.addAll(tryHttpResource(uri));
            } else {
                throw ConfigMessages.msg.schemeNotSupported(uri.getScheme());
            }
        }
        return configSources;
    }

    protected List<ConfigSource> tryFileSystem(final URI uri) {
        final List<ConfigSource> configSources = new ArrayList<>();
        final Path urlPath = uri.getScheme() != null ? Paths.get(uri) : Paths.get(uri.getPath());
        if (Files.isRegularFile(urlPath)) {
            consumeAsPath(toURL(urlPath.toUri()), new ConfigSourcePathConsumer(configSources));
        } else if (Files.isDirectory(urlPath)) {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(urlPath, this::validExtension)) {
                for (Path path : paths) {
                    addConfigSource(path.toUri(), configSources);
                }
            } catch (IOException e) {
                throw ConfigMessages.msg.failedToLoadResource(e);
            }
        }
        return configSources;
    }

    protected List<ConfigSource> tryClassPath(final URI uri, final ClassLoader classLoader) {
        final List<ConfigSource> configSources = new ArrayList<>();
        try {
            consumeAsPaths(uri.getPath(), new ConfigSourcePathConsumer(configSources));
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        } catch (IllegalArgumentException e) {
            configSources.addAll(fallbackToUnknownProtocol(uri, classLoader));
        }
        return configSources;
    }

    protected List<ConfigSource> tryJar(final URI uri) {
        final List<ConfigSource> configSources = new ArrayList<>();
        try {
            consumeAsPath(toURL(uri), new ConfigSourcePathConsumer(configSources));
        } catch (Exception e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        }
        return configSources;
    }

    protected List<ConfigSource> fallbackToUnknownProtocol(final URI uri, final ClassLoader classLoader) {
        final List<ConfigSource> configSources = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(uri.toString());
            while (resources.hasMoreElements()) {
                final URL resourceUrl = resources.nextElement();
                if (validExtension(resourceUrl.getFile())) {
                    final ConfigSource mainSource = addConfigSource(resourceUrl, configSources);
                    configSources.add(new ConfigurableConfigSource((ProfileConfigSourceFactory) profiles -> {
                        final List<ConfigSource> profileSources = new ArrayList<>();
                        for (int i = profiles.size() - 1; i >= 0; i--) {
                            final int ordinal = mainSource.getOrdinal() + profiles.size() - i + 1;
                            final URI profileUri = addProfileName(uri, profiles.get(i));
                            try {
                                final Enumeration<URL> profileResources = classLoader.getResources(profileUri.toString());
                                while (profileResources.hasMoreElements()) {
                                    final URL profileUrl = profileResources.nextElement();
                                    addProfileConfigSource(profileUrl, ordinal, profileSources);
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

    protected List<ConfigSource> tryHttpResource(final URI uri) {
        final List<ConfigSource> configSources = new ArrayList<>();
        if (validExtension(uri.getPath())) {
            final ConfigSource mainSource = addConfigSource(uri, configSources);
            configSources.addAll(tryProfiles(uri, mainSource));
        }
        return configSources;
    }

    protected List<ConfigSource> tryProfiles(final URI uri, final ConfigSource mainSource) {
        final List<ConfigSource> configSources = new ArrayList<>();
        configSources.add(new ConfigurableConfigSource((ProfileConfigSourceFactory) profiles -> {
            final List<ConfigSource> profileSources = new ArrayList<>();
            for (int i = profiles.size() - 1; i >= 0; i--) {
                final int ordinal = mainSource.getOrdinal() + profiles.size() - i + 1;
                final URI profileUri = addProfileName(uri, profiles.get(i));
                addProfileConfigSource(toURL(profileUri), ordinal, profileSources);
            }
            return profileSources;
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

    private ConfigSource addConfigSource(final URI uri, final List<ConfigSource> configSources) {
        return addConfigSource(toURL(uri), configSources);
    }

    private ConfigSource addConfigSource(final URL url, final List<ConfigSource> configSources) {
        try {
            final ConfigSource configSource = loadConfigSource(url);
            if (!configSource.getPropertyNames().isEmpty()) {
                configSources.add(configSource);
            }
            return configSource;
        } catch (IOException e) {
            throw ConfigMessages.msg.failedToLoadResource(e);
        }
    }

    private void addProfileConfigSource(final URL profileToFileName, final int ordinal,
            final List<ConfigSource> profileSources) {
        try {
            final ConfigSource configSource = loadConfigSource(profileToFileName, ordinal);
            if (!configSource.getPropertyNames().isEmpty()) {
                profileSources.add(configSource);
            }
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
            return URI.create("jar:" + addProfileName(URI.create(uri.getSchemeSpecificPart()), profile));
        }

        final String fileName = uri.getPath();
        assert fileName != null;

        final int dot = fileName.lastIndexOf(".");
        final String fileNameProfile;
        if (dot != -1) {
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

        public ConfigSourcePathConsumer(final List<ConfigSource> configSources) {
            this.configSources = configSources;
        }

        @Override
        public void accept(final Path path) {
            final AbstractLocationConfigSourceLoader loader = AbstractLocationConfigSourceLoader.this;
            if (loader.validExtension(path.getFileName().toString())) {
                final ConfigSource mainSource = loader.addConfigSource(path.toUri(), configSources);
                configSources.addAll(loader.tryProfiles(path.toUri(), mainSource));
            }
        }
    }

    interface ProfileConfigSourceFactory extends ConfigSourceFactory {
        @Override
        default Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
            final List<String> profiles = context.getProfiles();
            if (profiles.isEmpty()) {
                return Collections.emptyList();
            }

            return getProfileConfigSources(profiles);
        }

        Iterable<ConfigSource> getProfileConfigSources(final List<String> profiles);
    }
}
