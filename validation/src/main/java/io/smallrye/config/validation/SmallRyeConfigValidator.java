package io.smallrye.config.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraintvalidation.ValidationTarget;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorDescriptor;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.hibernate.validator.internal.metadata.core.ConstraintHelper;
import org.hibernate.validator.internal.util.TypeHelper;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.ConfigValue;

public class SmallRyeConfigValidator implements ConfigValidator {
    private static final long serialVersionUID = 1634006861872432627L;

    private final Validator validator;
    private final ConstraintHelper constraintHelper;

    public SmallRyeConfigValidator() {
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.constraintHelper = ConstraintHelper.forAllBuiltinConstraints();
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

    /**
     * Cannot use BVal here. There is not way to add dynamic valications without creating a new factory, which may
     * cause performance issues for each property that requires validation (plus an overkill). Also, the declarative
     * nature of BVal, is not suited for runtime validation, where a validation needs to be done on the fly with
     * the conversion type to validate and validation rules only known at runtime.
     *
     * Used some of the Hibernate BVal API to hook into the same validators, so we don't have to implement all of them,
     * but it is somehow a fragile approach.
     */
    public void validate(final ConfigValueValidatorBuilder validatorBuilder, final Object value)
            throws ConfigValidationException {

        validateMax(validatorBuilder, value);
        validateMin(validatorBuilder, value);
    }

    private <V> void validateMax(final ConfigValueValidatorBuilder validatorBuilder, final V value) {
        if (validatorBuilder.hasMax()) {
            final Optional<ConstraintValidatorDescriptor<Max>> suitableValidator = findSuitableValidator(Max.class,
                    value.getClass());

            // Ignore the validation if not suitable validator is present for the type being converted.
            if (suitableValidator.isPresent()) {
                final ConstraintValidatorDescriptor<Max> validatorDescriptor = suitableValidator.get();
                final ConstraintValidator<Max, V> maxConstraintValidator = (ConstraintValidator<Max, V>) validatorDescriptor
                        .newInstance(new ConstraintValidatorFactoryImpl());
                maxConstraintValidator.initialize(new Max() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }

                    @Override
                    public String message() {
                        return null;
                    }

                    @Override
                    public Class<?>[] groups() {
                        return null;
                    }

                    @Override
                    public Class<? extends Payload>[] payload() {
                        return null;
                    }

                    @Override
                    public long value() {
                        return validatorBuilder.getMax();
                    }
                });

                // Still need to figure out how to generate the message.
                if (!maxConstraintValidator.isValid(value, null)) {
                    throw new ConfigValidationException();
                }
            }
        }
    }

    private <V> void validateMin(final ConfigValueValidatorBuilder validatorBuilder, final V value) {
        if (validatorBuilder.hasMax()) {
            final Optional<ConstraintValidatorDescriptor<Min>> suitableValidator = findSuitableValidator(Min.class,
                    value.getClass());

            // Ignore the validation if not suitable validator is present for the type being converted.
            if (suitableValidator.isPresent()) {
                final ConstraintValidatorDescriptor<Min> validatorDescriptor = suitableValidator.get();
                final ConstraintValidator<Min, V> minConstraintValidator = (ConstraintValidator<Min, V>) validatorDescriptor
                        .newInstance(new ConstraintValidatorFactoryImpl());
                minConstraintValidator.initialize(new Min() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }

                    @Override
                    public String message() {
                        return null;
                    }

                    @Override
                    public Class<?>[] groups() {
                        return null;
                    }

                    @Override
                    public Class<? extends Payload>[] payload() {
                        return null;
                    }

                    @Override
                    public long value() {
                        return validatorBuilder.getMax();
                    }
                });

                // Still need to figure out how to generate the message.
                if (!minConstraintValidator.isValid(value, null)) {
                    throw new ConfigValidationException();
                }
            }
        }
    }

    private <A extends Annotation> Optional<ConstraintValidatorDescriptor<A>> findSuitableValidator(
            final Class<A> annotationType, final Type target) {
        final List<ConstraintValidatorDescriptor<A>> validatorDescriptors = constraintHelper
                .findValidatorDescriptors(annotationType, ValidationTarget.ANNOTATED_ELEMENT);

        for (ConstraintValidatorDescriptor<A> validatorDescriptor : validatorDescriptors) {
            if (TypeHelper.isAssignable(validatorDescriptor.getValidatedType(), target)) {
                return Optional.of(validatorDescriptor);
            }
        }

        return Optional.empty();
    }
}
