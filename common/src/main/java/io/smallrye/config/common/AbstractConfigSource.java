package io.smallrye.config.common;

import java.io.Serializable;

import org.eclipse.microprofile.config.spi.ConfigSource;

public abstract class AbstractConfigSource implements ConfigSource, Serializable {
    private static final long serialVersionUID = 9018847720072978115L;

    private final int ordinal;
    private final String name;

    public AbstractConfigSource(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
