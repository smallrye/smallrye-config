package io.smallrye.config.inject;

import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.SmallRyeConfig;

public class ConfigPropertiesInjectionBean<T> implements Bean<T> {
    private final ConfigClass configClass;
    private final Set<Annotation> qualifiers;

    ConfigPropertiesInjectionBean(final ConfigClass configClass) {
        this.configClass = configClass;
        this.qualifiers = Collections.singleton(ConfigProperties.Literal.of(configClass.getPrefix()));
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
        String prefix = configClass.getPrefix();
        if (prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
            prefix = configClass.getType().getAnnotation(ConfigProperties.class).prefix();
            if (prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
                prefix = "";
            }
        }

        SmallRyeConfig config = ConfigProvider.getConfig(getContextClassLoader()).unwrap(SmallRyeConfig.class);
        return config.getConfigMapping(getBeanClass(), prefix);
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
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName() + "_" + configClass.getType().getName() + "_"
                + configClass.getPrefix();
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
