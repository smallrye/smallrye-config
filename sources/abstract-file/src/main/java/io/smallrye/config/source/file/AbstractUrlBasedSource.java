package io.smallrye.config.source.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.smallrye.config.events.ChangeEventNotifier;
import io.smallrye.config.source.EnabledConfigSource;

/**
 * URL Based property files
 * 
 * Load some file from a file and convert to properties.
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public abstract class AbstractUrlBasedSource extends EnabledConfigSource implements Reloadable {
    private static final Logger log = Logger.getLogger(AbstractUrlBasedSource.class.getName());

    private final LinkedHashMap<URL, Map<String, String>> propertiesMap = new LinkedHashMap<>();
    private final Map<String, String> properties = new HashMap<>();
    private final String urlInputString;
    private final String keySeparator;
    private final boolean pollForChanges;
    private final int pollInterval;
    private final boolean notifyOnChanges;

    private final FileResourceWatcher fileResourceWatcher = new FileResourceWatcher();
    private final WebResourceWatcher webResourceWatcher = new WebResourceWatcher();

    public AbstractUrlBasedSource() {
        log.log(Level.INFO, "Loading [{0}] MicroProfile ConfigSource", getClass().getSimpleName());

        this.keySeparator = loadPropertyKeySeparator();
        this.notifyOnChanges = loadNotifyOnChanges();
        this.pollForChanges = loadPollForChanges();
        this.pollInterval = loadPollInterval();
        this.urlInputString = loadUrlPath();
        this.loadUrls(urlInputString);

        super.initOrdinal(500);
    }

    @Override
    protected Map<String, String> getPropertiesIfEnabled() {
        return this.properties;
    }

    @Override
    public String getValue(String key) {
        // in case we are about to configure ourselves we simply ignore that key
        if (super.isEnabled() && !key.startsWith(getKeyWithPrefix(null))) {
            return this.properties.get(key);
        }
        return null;
    }

    @Override
    public String getName() {
        return getClassKeyPrefix();
    }

    @Override
    public void reload(URL url) {
        Map<String, String> before = new HashMap<>(this.properties);
        initialLoad(url);
        mergeProperties();
        Map<String, String> after = new HashMap<>(this.properties);
        if (notifyOnChanges)
            ChangeEventNotifier.getInstance().detectChangesAndFire(before, after, getName());
    }

    private void initialLoad(URL url) {
        try (InputStream inputStream = url.openStream()) {
            if (inputStream != null) {
                this.propertiesMap.put(url, toMap(inputStream));
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to read URL [{0}] - {1}", new Object[] { url, e.getMessage() });
        }
    }

    protected String getKeySeparator() {
        return this.keySeparator;
    }

    private void loadUrls(String surl) {
        String urls[] = surl.split(COMMA);

        for (String u : urls) {
            if (u != null && !u.isEmpty()) {
                loadUrl(u.trim());
            }
        }
        mergeProperties();
    }

    private void loadUrl(String url) {
        try {
            URL u = new URL(url);
            log.log(Level.INFO, "Using [{0}] as {1} URL", new Object[] { u.toString(), getFileExtension() });
            initialLoad(u);

            if (shouldPollForChanges()) {
                // Local (file://...)
                if (isLocalResource(u)) {
                    this.fileResourceWatcher.startWatching(u, this);
                    // Web (http://...)
                } else if (isWebResource(u)) {
                    this.webResourceWatcher.startWatching(u, this, pollInterval);
                    // TODO: Add support for other protocols ?     
                } else {
                    log.log(Level.WARNING, "Can not detect changes on resource {0}", u.getFile());
                }
            }
        } catch (MalformedURLException ex) {
            log.log(Level.WARNING, "Can not load URL [" + url + "]", ex);
        }
    }

    private boolean isLocalResource(URL u) {
        return u.getProtocol().equalsIgnoreCase(FILE);
    }

    private boolean isWebResource(URL u) {
        return u.getProtocol().equalsIgnoreCase(HTTP) || u.getProtocol().equalsIgnoreCase(HTTPS);
    }

    private boolean shouldPollForChanges() {
        return this.pollForChanges && this.pollInterval > 0;
    }

    private void mergeProperties() {
        this.properties.clear();
        Set<Map.Entry<URL, Map<String, String>>> entrySet = propertiesMap.entrySet();
        for (Map.Entry<URL, Map<String, String>> entry : entrySet) {
            this.properties.putAll(entry.getValue());
        }
    }

    private String loadPropertyKeySeparator() {
        return config.getOptionalValue(getKeyWithPrefix(KEY_SEPARATOR), String.class).orElse(DOT);
    }

    private boolean loadPollForChanges() {
        return config.getOptionalValue(getKeyWithPrefix(POLL_FOR_CHANGES), Boolean.class).orElse(DEFAULT_POLL_FOR_CHANGES);
    }

    private boolean loadNotifyOnChanges() {
        return config.getOptionalValue(getKeyWithPrefix(NOTIFY_ON_CHANGES), Boolean.class).orElse(DEFAULT_NOTIFY_ON_CHANGES);
    }

    private int loadPollInterval() {
        return config.getOptionalValue(getKeyWithPrefix(POLL_INTERVAL), Integer.class).orElse(DEFAULT_POLL_INTERVAL);
    }

    private String loadUrlPath() {
        return config.getOptionalValue(getKeyWithPrefix(URL), String.class).orElse(getDefaultUrl());
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

    private static final String POLL_FOR_CHANGES = "pollForChanges";
    private static final boolean DEFAULT_POLL_FOR_CHANGES = false;

    private static final String NOTIFY_ON_CHANGES = "notifyOnChanges";
    private static final boolean DEFAULT_NOTIFY_ON_CHANGES = true;

    private static final String POLL_INTERVAL = "pollInterval";
    private static final int DEFAULT_POLL_INTERVAL = 5; // 5 seconds

    private static final String APPLICATION = "application";
    private static final String FILE = "file";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    protected abstract String getFileExtension();

    protected abstract Map<String, String> toMap(final InputStream inputStream);
}
