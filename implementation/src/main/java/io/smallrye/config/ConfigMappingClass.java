package io.smallrye.config;

import java.lang.reflect.Modifier;

import io.smallrye.common.constraint.Assert;

final class ConfigMappingClass implements ConfigMappingMetadata {
    private static final ClassValue<ConfigMappingClass> cv = new ClassValue<ConfigMappingClass>() {
        @Override
        protected ConfigMappingClass computeValue(final Class<?> classType) {
            return createConfigurationClass(classType);
        }
    };

    static ConfigMappingClass getConfigurationClass(Class<?> classType) {
        Assert.checkNotNullParam("classType", classType);
        return cv.get(classType);
    }

    private static ConfigMappingClass createConfigurationClass(final Class<?> classType) {
        if (classType.isInterface() && classType.getTypeParameters().length == 0 ||
                Modifier.isAbstract(classType.getModifiers()) ||
                classType.isEnum()) {
            return null;
        }

        return new ConfigMappingClass(classType);
    }

    private final Class<?> classType;
    private final String interfaceName;

    public ConfigMappingClass(final Class<?> classType) {
        this.classType = classType;
        this.interfaceName = ConfigMappingGenerator.generateInterfaceName(classType);
    }

    @Override
    public Class<?> getInterfaceType() {
        return classType;
    }

    @Override
    public String getClassName() {
        return interfaceName;
    }

    @Override
    public byte[] getClassBytes() {
        return ConfigMappingGenerator.generate(classType, interfaceName);
    }
}
