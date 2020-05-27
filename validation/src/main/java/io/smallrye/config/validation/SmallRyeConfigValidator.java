package io.smallrye.config.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.ConfigValue;

public class SmallRyeConfigValidator implements ConfigValidator {
    private static final long serialVersionUID = 1634006861872432627L;

    private final Validator validator;

    public SmallRyeConfigValidator() {
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    public void validate(final Class<?> klass, final String propertyName, final Object value)
            throws ConfigValidationException {

        final Set<? extends ConstraintViolation<?>> constraintViolations = validator.validateValue(klass, propertyName, value);

        if (!constraintViolations.isEmpty()) {
            // TODO - radcortez - merge?
            final ConstraintViolation<?> first = constraintViolations.iterator().next();
            throw new ConfigValidationException(first.getMessage());
        }
    }

    @Override
    public void validate(final ConfigValue configValue) throws ConfigValidationException {
        // Probably cannot use Bval here. No way to create dynamic validations without a new factory, which may be an
        // overkill for each property that needs validation.
    }
}
