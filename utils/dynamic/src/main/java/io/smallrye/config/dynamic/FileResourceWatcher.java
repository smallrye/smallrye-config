package io.smallrye.config.dynamic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.config.events.ChangeEventNotifier;
import io.smallrye.config.source.file.AbstractUrlBasedSource;

/**
 * Watching files for changes
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ApplicationScoped
public class FileResourceWatcher {
    private static final Logger log = Logger.getLogger(FileResourceWatcher.class.getName());

    @Inject
    private ChangeEventNotifier changeEventNotifier;

    public void startWatching(URL url, AbstractUrlBasedSource source, boolean notifyOnChanges) {

        CompletableFuture.runAsync(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path fileWeAreWatching = Paths.get(url.toURI());
                Path dir = fileWeAreWatching.getParent();
                String filename = fileWeAreWatching.getFileName().toString();

                log.log(Level.INFO, "Monitoring [{0}] in {1} for changes", new Object[] { filename, dir });
                dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileThatChanged = dir.resolve(ev.context());

                        if (fileThatChanged.equals(fileWeAreWatching)) {
                            Map<String, String> before = new HashMap<>(source.getProperties());
                            source.reload(url);
                            Map<String, String> after = new HashMap<>(source.getProperties());
                            if (notifyOnChanges)
                                changeEventNotifier.detectChangesAndFire(before, after, source.getName());
                        }
                    }
                    key.reset();
                }

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.log(Level.WARNING, "Can not watch url [" + url + "]", ex);
            } catch (URISyntaxException | IOException ex) {
                log.log(Level.WARNING, "Can not watch url [" + url + "]", ex);
            }
        });
    }
}
