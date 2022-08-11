package io.smallrye.config.inject;

import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;
import io.smallrye.config.SmallRyeConfig;

public class ConfigPropertiesInjectionBean<T> implements Bean<T> {
    private final ConfigClassWithPrefix configClassWithPrefix;
    private final Set<Annotation> qualifiers;

    ConfigPropertiesInjectionBean(final ConfigClassWithPrefix configClassWithPrefix) {
        this.configClassWithPrefix = configClassWithPrefix;
        this.qualifiers = Collections.singleton(ConfigProperties.Literal.of(configClassWithPrefix.getPrefix()));
    }

    @Override
    public Class<T> getBeanClass() {
        return (Class<T>) configClassWithPrefix.getKlass();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(final CreationalContext<T> creationalContext) {
        String prefix = configClassWithPrefix.getPrefix();
        if (prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
            prefix = configClassWithPrefix.getKlass().getAnnotation(ConfigProperties.class).prefix();
            if (prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
                prefix = "";
            }
        }

        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig(getContextClassLoader());
        return config.getConfigMapping(getBeanClass(), prefix);
    }

    @Override
    public void destroy(final T instance, final CreationalContext<T> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(configClassWithPrefix.getKlass());
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName() + "_" + configClassWithPrefix.getKlass().getName() + "_"
                + configClassWithPrefix.getPrefix();
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
