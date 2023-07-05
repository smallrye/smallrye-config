package io.smallrye.config.validator.external;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.validator.BeanValidationConfigValidatorImpl;

public class ValidationVisibilityTest {
    @Test
    void visibility() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("default.visibility.value", "12345678")
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withMapping(DefaultVisibility.class)
                .build();

        DefaultVisibility mapping = config.getConfigMapping(DefaultVisibility.class);

        assertEquals("12345678", mapping.value());
    }

    @ConfigMapping(prefix = "default.visibility")
    interface DefaultVisibility {
        @Length(max = 10)
        String value();
    }
}
