package io.smallrye.config.source.file;

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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watching files for changes
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class FileResourceWatcher {
    private static final Logger log = Logger.getLogger(FileResourceWatcher.class.getName());

    public void startWatching(URL url, Reloadable reloadable) {

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
                            reloadable.reload(url);
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