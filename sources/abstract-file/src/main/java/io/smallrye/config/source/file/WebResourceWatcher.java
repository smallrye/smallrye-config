package io.smallrye.config.source.file;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watching web resources for changes
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class WebResourceWatcher {
    private static final Logger log = Logger.getLogger(WebResourceWatcher.class.getName());

    private final ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();

    public void startWatching(URL url, Reloadable reloadable, long pollInterval) {
        if (!urlsToWatch.containsKey(url)) {
            long lastModified = getLastModified(url);
            if (lastModified > 0) {
                scheduledThreadPool.scheduleAtFixedRate(() -> {
                    long latestLastModified = getLastModified(url);
                    if (latestLastModified != urlsToWatch.get(url)) {
                        urlsToWatch.put(url, latestLastModified);
                        reloadable.reload(url);
                    }
                }, pollInterval, pollInterval, TimeUnit.SECONDS);

                urlsToWatch.put(url, lastModified);
            } else {
                log.log(Level.WARNING, "Can not poll {0} for changes, lastModified not implemented", url);
            }
        }
    }

    private long getLastModified(URL url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(HEAD);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            return con.getLastModified();
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage());
        } finally {
            if (con != null)
                con.disconnect();
        }

        return -1;
    }

    private static final String HEAD = "HEAD";

    private final Map<URL, Long> urlsToWatch = new ConcurrentHashMap<>();
}