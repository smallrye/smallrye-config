package io.smallrye.config.examples.mapping;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;

public class DurationConverter implements Converter<Duration> {
    private static final long serialVersionUID = 7499347081928776532L;
    private static final String PERIOD_OF_TIME = "PT";
    private static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");
    private static final Pattern START_WITH_DIGITS = Pattern.compile("^[-+]?\\d+.*");

    @Override
    public Duration convert(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (DIGITS.asPredicate().test(value)) {
            return Duration.ofSeconds(Long.parseLong(value));
        }

        try {
            if (START_WITH_DIGITS.asPredicate().test(value)) {
                return Duration.parse(PERIOD_OF_TIME + value);
            }

            return Duration.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
