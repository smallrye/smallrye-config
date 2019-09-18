package io.smallrye.config.source.file;

import java.net.URL;

/**
 * Marks a class as reloadable
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public interface Reloadable {
    public void reload(URL url);
}
