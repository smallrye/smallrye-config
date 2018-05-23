/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.config.inject;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import io.smallrye.config.SmallRyeConfig;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ConfigInjectionBean<T> implements Bean<T>, PassivationCapable {

    private final static Set<Annotation> QUALIFIERS = new HashSet<>();
    static {
        QUALIFIERS.add(new ConfigPropertyLiteral());
    }

    private final BeanManager bm;
    private final Class clazz;

    /**
     * only access via {@link #getConfig(}
     */
    private Config _config;

    public ConfigInjectionBean(BeanManager bm, Class clazz) {
        this.bm = bm;
        this.clazz = clazz;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Class<?> getBeanClass() {
        return ConfigInjectionBean.class;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(CreationalContext<T> context) {
        InjectionPoint ip = (InjectionPoint) bm.getInjectableReference(new InjectionPointMetadataInjectionPoint(), context);
        Annotated annotated = ip.getAnnotated();
        ConfigProperty configProperty = annotated.getAnnotation(ConfigProperty.class);
        String key = ConfigExtension.getConfigKey(ip, configProperty);
        String defaultValue = configProperty.defaultValue();

        if (annotated.getBaseType() instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) annotated.getBaseType();
            Type rawType = paramType.getRawType();

            // handle Provider<T>
            if (rawType instanceof Class && ((Class) rawType).isAssignableFrom(Provider.class) && paramType.getActualTypeArguments().length == 1) {
                Class clazz = (Class) paramType.getActualTypeArguments()[0]; //X TODO check type again, etc
                return (T) new ConfigValueProvider(getConfig(), key, clazz);
            }
        } else {
            Class clazz = (Class) annotated.getBaseType();
            if (defaultValue == null || defaultValue.length() == 0) {
                Object value = getConfig().getValue(key, clazz);
                return (T) getConfig().getValue(key, clazz);
            } else {
                Config config = getConfig();
                Optional<T> optionalValue = config.getOptionalValue(key, clazz);
                if (optionalValue.isPresent()) {
                    return optionalValue.get();
                } else {
                    return (T)((SmallRyeConfig) config).convert(defaultValue, clazz);
                }
            }
        }

        throw new IllegalStateException("unhandled ConfigProperty");
    }

    public Config getConfig() {
        if (_config == null) {
            _config = ConfigProvider.getConfig();
        }
        return _config;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> context) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(clazz);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return "ConfigInjectionBean_" + clazz;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return "ConfigInjectionBean_" + clazz;
    }

    private static class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty> implements ConfigProperty {
        @Override
        public String name() {
            return "";
        }

        @Override
        public String defaultValue() {
            return "";
        }
    }

    public static class ConfigValueProvider<T> implements Provider<T>, Serializable {
        private transient Config config;
        private final String key;
        private final Class<T> type;

        ConfigValueProvider(Config config, String key, Class<T> type) {
            this.config = config;
            this.key = key;
            this.type = type;
        }

        @Override
        public T get() {
            return (T) config.getValue(key, type);
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            config = ConfigProviderResolver.instance().getConfig();
        }

    }

    private static class InjectionPointMetadataInjectionPoint implements InjectionPoint {

        @Override
        public Type getType() {
            return InjectionPoint.class;
        }

        @SuppressWarnings("serial")
        @Override
        public Set<Annotation> getQualifiers() {
            return Collections.<Annotation> singleton(new AnnotationLiteral<Default>() {
            });
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return null;
        }

        @Override
        public Annotated getAnnotated() {
            return null;
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

    }
}
