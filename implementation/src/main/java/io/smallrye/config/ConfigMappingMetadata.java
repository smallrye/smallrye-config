package io.smallrye.config;

import java.util.List;

public interface ConfigMappingMetadata {
    Class<?> getInterfaceType();

    String getClassName();

    byte[] getClassBytes();

    default List<? extends ConfigMappingMetadata> getNested() {
        return List.of();
    }
}
