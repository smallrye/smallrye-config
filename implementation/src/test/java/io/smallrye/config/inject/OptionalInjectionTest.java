package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
public class OptionalInjectionTest extends InjectionTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigProducer.class)
            .addBeans()
            .inject(this)
            .build();

    @Inject
    @ConfigProperty(name = "optional.int.value")
    private OptionalInt optionalInt;

    @Inject
    @ConfigProperty(name = "optional.long.value")
    private OptionalLong optionalLong;

    @Inject
    @ConfigProperty(name = "optional.double.value")
    private OptionalDouble optionalDouble;

    @Test
    public void optionalIntInjection() {
        assertTrue(optionalInt.isPresent());
        assertEquals(1, optionalInt.getAsInt());

        assertTrue(optionalLong.isPresent());
        assertEquals(2, optionalLong.getAsLong());

        assertTrue(optionalDouble.isPresent());
        assertEquals(3.3, optionalDouble.getAsDouble(), 0);
    }
}
