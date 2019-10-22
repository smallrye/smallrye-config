package io.smallrye.config.source.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.smallrye.config.source.EnabledConfigSource;

/**
 * URL Based property files
 * 
 * Load some file from a file and convert to properties.
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public abstract class AbstractUrlBasedSource extends EnabledConfigSource {
    private static final Logger LOG = Logger.getLogger(AbstractUrlBasedSource.class.getName());

    private final LinkedHashMap<URL, Map<String, String>> propertiesMap = new LinkedHashMap<>();
    private final Map<String, String> properties = new HashMap<>();
    private final List<URL> urls;
    private final String keySeparator;

    public AbstractUrlBasedSource() {
        LOG.log(Level.INFO, "Loading [{0}] MicroProfile ConfigSource", getClass().getSimpleName());
        this.keySeparator = loadPropertyKeySeparator();
        this.urls = loadUrls();
        super.initOrdinal(500);
    }

    @Override
    protected Map<String, String> getPropertiesIfEnabled() {
        return this.properties;
    }

    @Override
    public String getValue(String key) {
        // in case we are about to configure ourselves we simply ignore that key
        if (super.isEnabled() && !key.startsWith(getPrefix())) {
            return this.properties.get(key);
        }
        return null;
    }

    @Override
    public String getName() {
        return getClassKeyPrefix();
    }

    public List<URL> getUrlList() {
        return this.urls;
    }

    public void reload(URL url) {
        initialLoad(url);
        mergeProperties();
    }

    private void initialLoad(URL url) {
        try (InputStream inputStream = url.openStream()) {
            if (inputStream != null) {
                this.propertiesMap.put(url, toMap(inputStream));
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Unable to read URL [{0}] - {1}", new Object[] { url, e.getMessage() });
        }
    }

    protected String getKeySeparator() {
        return this.keySeparator;
    }

    private List<URL> loadUrls() {
        String inputConfig = config.getOptionalValue(getPrefixWithKey(URL), String.class).orElse(getDefaultUrl());

        List<URL> listOfValidUrls = new ArrayList<>();
        String values[] = inputConfig.split(COMMA);

        for (String u : values) {
            if (u != null && !u.isEmpty()) {
                URL url = loadUrl(u.trim());
                if (url != null)
                    listOfValidUrls.add(url);
            }
        }
        mergeProperties();
        return listOfValidUrls;
    }

    private URL loadUrl(String url) {
        try {
            URL u = new URL(url);
            LOG.log(Level.INFO, "Using [{0}] as {1} URL", new Object[] { u.toString(), getFileExtension() });
            initialLoad(u);
            return u;
        } catch (MalformedURLException ex) {
            LOG.log(Level.WARNING, "Can not load URL [" + url + "]", ex);
            return null;
        }
    }

    private void mergeProperties() {
        this.properties.clear();
        Set<Map.Entry<URL, Map<String, String>>> entrySet = propertiesMap.entrySet();
        for (Map.Entry<URL, Map<String, String>> entry : entrySet) {
            this.properties.putAll(entry.getValue());
        }
    }

    private String loadPropertyKeySeparator() {
        return config.getOptionalValue(getPrefixWithKey(KEY_SEPARATOR), String.class).orElse(DOT);
    }

    private String getDefaultUrl() {
        String path = APPLICATION + DOT + getFileExtension();
        try {
            URL u = Paths.get(path).toUri().toURL();
            return u.toString();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final String COMMA = ",";
    private static final String URL = "url";
    private static final String KEY_SEPARATOR = "keyseparator";

    private static final String APPLICATION = "application";

    protected abstract String getFileExtension();

    protected abstract Map<String, String> toMap(final InputStream inputStream);
}
