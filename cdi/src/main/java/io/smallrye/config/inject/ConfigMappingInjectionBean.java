package io.smallrye.config.inject;

import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.smallrye.config.Config;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings.ConfigClass;

public class ConfigMappingInjectionBean<T> implements Bean<T> {
    private final BeanManager bm;
    private final ConfigClass configClass;

    public ConfigMappingInjectionBean(final ConfigClass configClass, final BeanManager bm) {
        this.bm = bm;
        this.configClass = configClass;
    }

    @Override
    public Class<T> getBeanClass() {
        return (Class<T>) configClass.getType();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public T create(final CreationalContext<T> creationalContext) {
        InjectionPoint injectionPoint = (InjectionPoint) bm.getInjectableReference(new MetadataInjectionPoint(),
                creationalContext);

        String prefix = configClass.getPrefix();
        if (injectionPoint != null && injectionPoint.getAnnotated() != null) {
            ConfigMapping configMapping = injectionPoint.getAnnotated().getAnnotation(ConfigMapping.class);
            if (configMapping != null) {
                prefix = configMapping.prefix();
            }
        }

        return Config.get(getContextClassLoader()).getConfigMapping(getBeanClass(), prefix);
    }

    @Override
    public void destroy(final T instance, final CreationalContext<T> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(configClass.getType());
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.singleton(Default.Literal.INSTANCE);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName() + "_" + configClass.getType().getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }
}
