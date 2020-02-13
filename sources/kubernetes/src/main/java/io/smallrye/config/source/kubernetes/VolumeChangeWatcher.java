package io.smallrye.config.source.kubernetes;

import io.smallrye.config.source.kubernetes.cdi.ChangeConfigEvent;
import io.smallrye.config.source.kubernetes.cdi.ChangeEventNotifier;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import org.jboss.logging.Logger;

public class VolumeChangeWatcher implements Runnable {

    private static final Logger LOG = Logger.getLogger("io.smallrye.config");
    private KubernetesConfigSource kubernetesConfigSource;

    private WatchService watcher;

    VolumeChangeWatcher(final KubernetesConfigSource kubernetesConfigSource) {
        this.kubernetesConfigSource = kubernetesConfigSource;
        registerWatchService();
    }

    private void registerWatchService() {
        try {
            watcher = kubernetesConfigSource.volumePath.getFileSystem().newWatchService();
            kubernetesConfigSource.volumePath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            LOG.warnf("Unable to get the WatchService to inspect any change that occurs on %s. Exception: %s",
                this.kubernetesConfigSource.volumePath.toString(), e.getLocalizedMessage());
            throw new IllegalStateException(e);
        }
    }

    public void run() {
        try {
            WatchKey watchKey;
            while ((watchKey = watcher.take()) != null) {
                    List<WatchEvent<?>> events = watchKey.pollEvents();
                    for (WatchEvent<?> event : events) {
                        final WatchEvent<Path> eventPath = (WatchEvent<Path>) event;
                        Path dir = (Path)watchKey.watchable();
                        Path fileName = dir.resolve(eventPath.context());
                        boolean updated = this.kubernetesConfigSource.populateProperties(fileName, this.kubernetesConfigSource.initConfigSourceParserChain());
                        // If not updated means that it is a post config source chain.
                        if (!updated) {
                            this.kubernetesConfigSource.postConfigSource();
                        }

                        final ChangeEventNotifier changeEventNotifier = ChangeEventNotifier.getInstance();
                        if (isInsideCdiContainer(changeEventNotifier)) {
                            changeEventNotifier.fire(new ChangeConfigEvent(fileName));
                        }

                        LOG.debugf("%s Event Happened on %s", event.kind(), event.context());
                    }
                    watchKey.reset();
            }
        } catch(InterruptedException e) {
            LOG.warnf("Volume Change Watcher has been interrupted. Exception: %s.",
                this.kubernetesConfigSource.volumePath.toString());
            throw new IllegalStateException(e);
        }
    }

    private boolean isInsideCdiContainer(ChangeEventNotifier changeEventNotifier) {
        return changeEventNotifier != null;
    }
}
