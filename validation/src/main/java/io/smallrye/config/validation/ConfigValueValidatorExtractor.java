package io.smallrye.config.validation;

import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.ValueExtractor;

public class ConfigValueValidatorExtractor implements ValueExtractor<ConfigValueValidator<@ExtractedValue ?>> {
    @Override
    public void extractValues(final ConfigValueValidator<?> originalValue, final ValueReceiver receiver) {
        System.out.println("ConfigValueValidatorExtractor.extractValues");
        receiver.value(null, originalValue.getValue());
    }
}
