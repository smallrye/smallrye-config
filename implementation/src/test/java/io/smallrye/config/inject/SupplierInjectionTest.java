package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)

public class SupplierInjectionTest extends InjectionTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigProducer.class, SupplierBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    SupplierBean supplierBean;

    @Test
    public void supplier() {
        assertEquals("1234", supplierBean.getMyProp());
        assertEquals("1234", supplierBean.getSupplierMyProp().get());
        assertEquals(1234, supplierBean.getSupplierInteger().get().intValue());
        assertEquals(1234, supplierBean.getSupplierOptionalInteger().get().get().intValue());
    }

    @Test
    public void dynamicSupplier() {
        assertEquals(1, supplierBean.getSupplierDynamic().get().intValue());
        assertEquals(2, supplierBean.getSupplierDynamic().get().intValue());
        assertEquals(3, supplierBean.getSupplierDynamic().get().intValue());
    }

    @ApplicationScoped
    public static class SupplierBean {
        @Inject
        @ConfigProperty(name = "my.prop")
        private String myProp;
        @Inject
        @ConfigProperty(name = "my.prop")
        private Supplier<String> supplierMyProp;
        @Inject
        @ConfigProperty(name = "my.prop")
        private Supplier<Integer> supplierInteger;
        @Inject
        @ConfigProperty(name = "my.prop")
        private Supplier<Optional<Integer>> supplierOptionalInteger;
        @Inject
        @ConfigProperty(name = "my.counter")
        private Supplier<Integer> supplierDynamic;

        String getMyProp() {
            return myProp;
        }

        Supplier<String> getSupplierMyProp() {
            return supplierMyProp;
        }

        Supplier<Integer> getSupplierInteger() {
            return supplierInteger;
        }

        Supplier<Optional<Integer>> getSupplierOptionalInteger() {
            return supplierOptionalInteger;
        }

        Supplier<Integer> getSupplierDynamic() {
            return supplierDynamic;
        }
    }
}
