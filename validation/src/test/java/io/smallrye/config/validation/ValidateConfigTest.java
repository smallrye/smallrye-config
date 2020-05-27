package io.smallrye.config.validation;

import static org.junit.Assert.assertThrows;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.validation.constraints.Max;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.Rule;
import org.junit.Test;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.inject.ConfigProducer;

public class ValidateConfigTest extends InjectionTest {
    @Rule
    public WeldInitiator weld = WeldInitiator.from(ConfigProducer.class, MaxConfigValidation.class)
            .addBeans()
            .inject(this)
            .build();

    @Test
    public void max() {
        assertThrows(ConfigValidationException.class, () -> CDI.current().select(MaxConfigValidation.class).get());
    }

    @Dependent
    public static class MaxConfigValidation {
        @Inject
        @ConfigProperty(name = "my.prop")
        @Max(10)
        private Integer integer;

        public Integer getInteger() {
            return integer;
        }
    }
}
