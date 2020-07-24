package io.smallrye.config;

public interface ConfigMappingClassMapper {
    default Object map() {
        throw new UnsupportedOperationException();
    }
}
