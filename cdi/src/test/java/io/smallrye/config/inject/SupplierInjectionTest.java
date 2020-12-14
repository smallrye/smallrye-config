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
class SupplierInjectionTest extends InjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, SupplierBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    SupplierBean supplierBean;

    @Test
    void supplier() {
        assertEquals("1234", supplierBean.getMyProp());
        assertEquals("1234", supplierBean.getSupplierMyProp().get());
        assertEquals(1234, supplierBean.getSupplierInteger().get().intValue());
        assertEquals(1234, supplierBean.getSupplierOptionalInteger().get().get().intValue());
    }

    @Test
    void dynamicSupplier() {
        assertEquals(1, supplierBean.getSupplierDynamic().get().intValue());
        assertEquals(2, supplierBean.getSupplierDynamic().get().intValue());
        assertEquals(3, supplierBean.getSupplierDynamic().get().intValue());
    }

    @ApplicationScoped
    static class SupplierBean {
        @Inject
        @ConfigProperty(name = "my.prop")
        String myProp;
        @Inject
        @ConfigProperty(name = "my.prop")
        Supplier<String> supplierMyProp;
        @Inject
        @ConfigProperty(name = "my.prop")
        Supplier<Integer> supplierInteger;
        @Inject
        @ConfigProperty(name = "my.prop")
        Supplier<Optional<Integer>> supplierOptionalInteger;
        @Inject
        @ConfigProperty(name = "my.counter")
        Supplier<Integer> supplierDynamic;

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
