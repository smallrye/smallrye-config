package io.smallrye.config.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValidator;

public class SmallRyeConfigValidator implements ConfigValidator {
    private Validator validator;

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
}
