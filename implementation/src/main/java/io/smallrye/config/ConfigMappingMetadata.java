package io.smallrye.config;

public interface ConfigMappingMetadata {
    Class<?> getInterfaceType();

    String getClassName();

    byte[] getClassBytes();
}
