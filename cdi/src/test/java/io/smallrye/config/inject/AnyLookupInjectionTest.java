package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * {@code @Any} lookups ignore the {@link ConfigProperty} qualifier, so they also resolve the producers in
 * {@link ConfigProducer}. Those injection points carry no {@link ConfigProperty}, so the producers have no property
 * name to look up and must fail with a clear message instead of producing {@code null}.
 *
 * @see <a href="https://github.com/smallrye/smallrye-config/issues/1641">#1641</a>
 */
@ExtendWith(WeldJunit5Extension.class)
class AnyLookupInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MyBeanProducers.class, ConfigPropertyBean.class)
            .addBeans()
            .inject(this)
            .build();

    @Inject
    @Any
    Instance<Object> any;

    @Test
    void optionalOfUserBean() {
        // the case from #1641: user beans of type Optional<MyBean>, looked up with @Any
        assertRejected(any.select(new TypeLiteral<Optional<MyBean>>() {
        }));
        // workaround: qualify the user beans and include the qualifier in the lookup
        List<Optional<MyBean>> values = any.select(new TypeLiteral<Optional<MyBean>>() {
        }, NamedLiteral.of("mine")).stream().toList();
        assertEquals(List.of(Optional.of(new MyBean("mine"))), values);
    }

    @Test
    void scalars() {
        assertRejected(any.select(String.class));
        assertRejected(any.select(Integer.class));
        assertRejected(any.select(Long.class));
        assertRejected(any.select(Float.class));
        assertRejected(any.select(Double.class));
        assertRejected(any.select(Boolean.class));
        assertRejected(any.select(Short.class));
        assertRejected(any.select(Byte.class));
        assertRejected(any.select(Character.class));
    }

    @Test
    void optionals() {
        assertRejected(any.select(new TypeLiteral<Optional<String>>() {
        }));
        assertRejected(any.select(OptionalInt.class));
        assertRejected(any.select(OptionalLong.class));
        assertRejected(any.select(OptionalDouble.class));
    }

    @Test
    void collections() {
        assertRejected(any.select(new TypeLiteral<List<String>>() {
        }));
        assertRejected(any.select(new TypeLiteral<Set<String>>() {
        }));
        assertRejected(any.select(new TypeLiteral<Map<String, String>>() {
        }));
    }

    @Test
    void configValue() {
        assertRejected(any.select(ConfigValue.class));
        assertRejected(any.select(org.eclipse.microprofile.config.ConfigValue.class));
    }

    @Test
    void supplier() {
        // rejected when the Supplier is produced, not when it is called
        assertRejected(any.select(new TypeLiteral<Supplier<String>>() {
        }));
    }

    @Test
    void superTypes() {
        // String, Integer, Long, ... are all resolved by their super types too
        assertRejected(any.select(CharSequence.class));
        assertRejected(any.select(Number.class));
    }

    @Test
    void configPropertyStillInjected() {
        ConfigPropertyBean bean = weld.select(ConfigPropertyBean.class).get();
        assertEquals("x", bean.string);
        assertEquals(Optional.of("x"), bean.optional);
        assertEquals("x", bean.supplier.get());
        assertEquals("x", bean.configValue.getValue());
    }

    @Test
    void customType() {
        // ConfigInjectionBean (types not handled by ConfigProducer) declares no @Any, so it is not resolved
        assertEquals(1, weld.select(ConfigPropertyBean.class).get().custom.size());
        assertEquals(0, any.select(CustomType.class).stream().count());
    }

    /**
     * Every {@link ConfigProducer} bean resolved by the lookup must fail with a clear message instead of producing
     * {@code null}.
     */
    static void assertRejected(Instance<?> instance) {
        int configBeans = 0;
        for (Instance.Handle<?> handle : instance.handles()) {
            if (handle.getBean().getBeanClass().equals(ConfigProducer.class)) {
                configBeans++;
                IllegalStateException exception = assertThrows(IllegalStateException.class, handle::get,
                        () -> handle.getBean() + " did not reject an @Any lookup");
                assertTrue(exception.getMessage().startsWith("SRCFG02008"), exception.getMessage());
            } else {
                assertNotNull(handle.get());
            }
        }
        assertTrue(configBeans > 0, "the lookup did not resolve any ConfigProducer bean");
    }

    public record MyBean(String name) {
    }

    @Dependent
    public static class MyBeanProducers {
        @Produces
        @Dependent
        static Optional<MyBean> optional() {
            return Optional.of(new MyBean("optional"));
        }

        @Produces
        @Dependent
        @Named("mine")
        static Optional<MyBean> mine() {
            return Optional.of(new MyBean("mine"));
        }
    }

    public static class CustomType {
        final String value;

        CustomType(String value) {
            this.value = value;
        }

        public static CustomType of(String value) {
            return new CustomType(value);
        }

        int size() {
            return value.length();
        }
    }

    @Dependent
    public static class ConfigPropertyBean {
        @Inject
        @ConfigProperty(name = "custom")
        String string;
        @Inject
        @ConfigProperty(name = "custom")
        Optional<String> optional;
        @Inject
        @ConfigProperty(name = "custom")
        Supplier<String> supplier;
        @Inject
        @ConfigProperty(name = "custom")
        ConfigValue configValue;
        @Inject
        @ConfigProperty(name = "custom")
        CustomType custom;
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("custom", "x"))
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProviderResolver.instance().getConfig());
    }
}
