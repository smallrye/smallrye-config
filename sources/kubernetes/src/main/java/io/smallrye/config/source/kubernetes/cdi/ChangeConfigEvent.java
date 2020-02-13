package io.smallrye.config.source.kubernetes.cdi;

import java.io.Serializable;
import java.nio.file.Path;

public class ChangeConfigEvent implements Serializable {

    private Path fromPath;

    public ChangeConfigEvent(final Path fromPath) {
        this.fromPath = fromPath;
    }

    public Path getFromPath() {
        return this.fromPath;
    }
}
