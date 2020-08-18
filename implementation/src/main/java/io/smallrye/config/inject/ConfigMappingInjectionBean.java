package io.smallrye.config.inject;

import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;

public class ConfigMappingInjectionBean<T> implements Bean<T> {
    private final BeanManager bm;
    private final Class<T> klass;
    private final String prefix;
    private final Set<Annotation> qualifiers = new HashSet<>();

    public ConfigMappingInjectionBean(final BeanManager bm, final AnnotatedType<T> type) {
        this.bm = bm;
        this.klass = type.getJavaClass();
        this.prefix = getConfigMappingPrefix(type);
        this.qualifiers.add(Default.Literal.INSTANCE);
        if (type.isAnnotationPresent(ConfigProperties.class)) {
            this.qualifiers.add(ConfigProperties.Literal.of(prefix));
        }
    }

    @Override
    public Class<?> getBeanClass() {
        return klass;
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
        InjectionPoint injectionPoint = (InjectionPoint) bm.getInjectableReference(new MetadataInjectionPoint(),
                creationalContext);

        final String overridePrefix;
        if (injectionPoint.getAnnotated() != null &&
                injectionPoint.getAnnotated().isAnnotationPresent(ConfigMapping.class)) {
            overridePrefix = getConfigMappingPrefix(injectionPoint.getAnnotated());
        } else if (!injectionPoint.getQualifiers().isEmpty()) {
            overridePrefix = injectionPoint.getQualifiers()
                    .stream()
                    .filter(ConfigProperties.class::isInstance)
                    .map(ConfigProperties.class::cast)
                    .map(ConfigProperties::prefix)
                    .findFirst()
                    .orElse(prefix);

        } else {
            overridePrefix = prefix;
        }

        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig(getContextClassLoader());
        return config.getConfigMapping(klass, overridePrefix);
    }

    @Override
    public void destroy(final T instance, final CreationalContext<T> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(klass);
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
        return this.getClass() + "_" + klass.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    static String getConfigMappingPrefix(final Annotated annotated) {
        return Optional.ofNullable(annotated.getAnnotation(ConfigMapping.class))
                .map(ConfigMapping::prefix)
                .orElseGet(
                        () -> Optional.ofNullable(annotated.getAnnotation(ConfigProperties.class)).map(ConfigProperties::prefix)
                                .orElse(""));
    }

}
