package io.smallrye.config.events;

import java.io.Serializable;
import java.util.Optional;

/**
 * an Event on a config element
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class ChangeEvent implements Serializable {

    private final Type type;
    private final String key;
    private final Optional<String> oldValue;
    private final String newValue;
    private final String fromSource;

    public ChangeEvent(Type type, String key, Optional<String> oldValue, String newValue, String fromSource) {
        this.type = type;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.fromSource = fromSource;
    }

    public Type getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public Optional<String> getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getFromSource() {
        return fromSource;
    }

    @Override
    public String toString() {
        return "ChangeEvent{" + "type=" + type + ", key=" + key + ", oldValue=" + oldValue + ", newValue=" + newValue
                + ", fromSource=" + fromSource + '}';
    }
}
