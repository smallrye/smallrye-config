package io.smallrye.config.dynamic;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.injection.ConfigSourceMap;
import io.smallrye.config.source.file.AbstractUrlBasedSource;

/**
 * Watching for changes in files
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ApplicationScoped
public class ResourceWatcher {
    private static final Logger log = Logger.getLogger(ResourceWatcher.class.getName());

    @Inject
    private Config config;

    @Inject
    @ConfigSourceMap
    private Map<String, ConfigSource> configSourceMap;

    @Inject
    private FileResourceWatcher fileResourceWatcher;

    @Inject
    private WebResourceWatcher webResourceWatcher;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        for (Map.Entry<String, ConfigSource> configSourceEntry : configSourceMap.entrySet()) {

            Class<? extends ConfigSource> configSourceClass = configSourceEntry.getValue().getClass();
            // First see if this is a File bases config source.
            boolean isAbstractUrlBasedSource = extendsAbstractUrlBasedSource(configSourceClass);
            // Then see if it's configured to be dynamic
            boolean pollForChanges = loadPollForChanges(configSourceClass);
            // Now check the poll interval. Has to more than 0
            int pollInterval = loadPollInterval(configSourceClass);
            // Also see if we need to notify on detected changed
            boolean notifyOnChanges = loadNotifyOnChanges(configSourceClass);
            if (isAbstractUrlBasedSource && pollForChanges && pollInterval > 0) {
                // Now get all the URL's and start watching
                AbstractUrlBasedSource source = (AbstractUrlBasedSource) configSourceEntry.getValue();
                List<URL> urls = source.getUrlList();
                for (URL url : urls) {
                    if (isLocalResource(url)) {
                        this.fileResourceWatcher.startWatching(url, source, notifyOnChanges);
                    } else if (isWebResource(url)) {
                        this.webResourceWatcher.startWatching(url, source, notifyOnChanges, pollInterval);
                    } else {
                        log.log(Level.WARNING, "Can not detect changes on resource {0}", url.getFile());
                    }
                }
            }
        }

    }

    private boolean isLocalResource(URL u) {
        return u.getProtocol().equalsIgnoreCase(FILE);
    }

    private boolean isWebResource(URL u) {
        return u.getProtocol().equalsIgnoreCase(HTTP) || u.getProtocol().equalsIgnoreCase(HTTPS);
    }

    private boolean extendsAbstractUrlBasedSource(Class configsource) {
        if (configsource.getName().equals(Object.class.getName()))
            return false;
        if (configsource.getName().equals(AbstractUrlBasedSource.class.getName()))
            return true;
        return extendsAbstractUrlBasedSource(configsource.getSuperclass());
    }

    private boolean loadPollForChanges(Class clazz) {
        String key = getKeyWithPrefix(clazz, POLL_FOR_CHANGES);
        if (configPropertyIsSet(key)) {
            return config.getOptionalValue(key, Boolean.class).get();
        } else {
            return DEFAULT_POLL_FOR_CHANGES;
        }
    }

    private int loadPollInterval(Class clazz) {
        String key = getKeyWithPrefix(clazz, POLL_INTERVAL);
        if (configPropertyIsSet(key)) {
            return config.getOptionalValue(key, Integer.class).get();
        } else {
            return DEFAULT_POLL_INTERVAL;
        }
    }

    private boolean loadNotifyOnChanges(Class clazz) {
        String key = getKeyWithPrefix(clazz, NOTIFY_ON_CHANGES);
        if (configPropertyIsSet(key)) {
            return config.getOptionalValue(key, Boolean.class).get();
        } else {
            return DEFAULT_NOTIFY_ON_CHANGES;
        }
    }

    private String getKeyWithPrefix(Class clazz, String key) {
        String prefix = clazz.getPackage().getName() + DOT;
        if (key == null)
            return prefix;
        return prefix + key;
    }

    private boolean configPropertyIsSet(String key) {
        for (String name : config.getPropertyNames()) {
            if (name.equals(key))
                return true;
        }
        return false;
    }

    private static final String DOT = ".";
    private static final String POLL_FOR_CHANGES = "pollForChanges";
    private static final boolean DEFAULT_POLL_FOR_CHANGES = false;

    private static final String POLL_INTERVAL = "pollInterval";
    private static final int DEFAULT_POLL_INTERVAL = 5; // 5 seconds

    private static final String NOTIFY_ON_CHANGES = "notifyOnChanges";
    private static final boolean DEFAULT_NOTIFY_ON_CHANGES = true;

    private static final String FILE = "file";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
}
