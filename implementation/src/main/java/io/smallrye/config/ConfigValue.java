package io.smallrye.config;

import static io.smallrye.config.DefaultValuesConfigSource.NAME;
import static io.smallrye.config.DefaultValuesConfigSource.ORDINAL;
import static io.smallrye.config.ProfileConfigSourceInterceptor.convertProfile;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.smallrye.config.ConfigValidationException.Problem;

/**
 * The ConfigValue is a metadata object that holds additional information after the lookup of a configuration.
 * <p>
 *
 * Right now, it is able to hold information like the configuration name, value, the Config Source from where
 * the configuration was loaded, the ordinal of the Config Source and a line number from where the configuration was
 * read if exists.
 * <p>
 *
 * This is used together with {@link ConfigValueConfigSource} and {@link ConfigSourceInterceptor} to expose the
 * Configuration lookup metadata.
 */
public class ConfigValue implements org.eclipse.microprofile.config.ConfigValue {
    private final String name;
    private final String value;
    private final String rawValue;
    private final String profile;
    private final String configSourceName;
    private final int configSourceOrdinal;
    private final int configSourcePosition;
    private final int lineNumber;

    private final List<Problem> problems;

    private ConfigValue(final ConfigValueBuilder builder) {
        this.name = builder.name;
        this.value = builder.value;
        this.rawValue = builder.rawValue;
        this.profile = builder.profile;
        this.configSourceName = builder.configSourceName;
        this.configSourceOrdinal = builder.configSourceOrdinal;
        this.configSourcePosition = builder.configSourcePosition;
        this.lineNumber = builder.lineNumber;
        this.problems = builder.problems;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getNameProfiled() {
        return profile != null ? "%" + profile + "." + name : name;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String getValueOrDefault(final String defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Override
    public String getRawValue() {
        return rawValue;
    }

    public String getProfile() {
        return profile;
    }

    public boolean isDefault() {
        return configSourceOrdinal == ORDINAL && NAME.equals(configSourceName);
    }

    @Override
    public String getSourceName() {
        return getConfigSourceName();
    }

    @Override
    public int getSourceOrdinal() {
        return getConfigSourceOrdinal();
    }

    public String getConfigSourceName() {
        return configSourceName;
    }

    public int getConfigSourceOrdinal() {
        return configSourceOrdinal;
    }

    public int getConfigSourcePosition() {
        return configSourcePosition;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLocation() {
        return lineNumber != -1 ? configSourceName + ":" + lineNumber : configSourceName;
    }

    public boolean hasProblems() {
        return problems != null && !problems.isEmpty();
    }

    public List<Problem> getProblems() {
        return hasProblems() ? unmodifiableList(problems) : emptyList();
    }

    public ConfigValue withName(final String name) {
        return from().withName(name).build();
    }

    public ConfigValue withValue(final String value) {
        return from().withValue(value).build();
    }

    public ConfigValue withProfile(final String profile) {
        return from().withProfile(profile).build();
    }

    public ConfigValue withConfigSourceName(final String configSourceName) {
        return from().withConfigSourceName(configSourceName).build();
    }

    public ConfigValue withConfigSourceOrdinal(final int configSourceOrdinal) {
        return from().withConfigSourceOrdinal(configSourceOrdinal).build();
    }

    public ConfigValue withConfigSourcePosition(final int configSourcePosition) {
        return from().withConfigSourcePosition(configSourcePosition).build();
    }

    public ConfigValue withLineNumber(final int lineNumber) {
        return from().withLineNumber(lineNumber).build();
    }

    public ConfigValue noProblems() {
        return from().noProblems().build();
    }

    public ConfigValue withProblems(final List<Problem> problems) {
        return from().withProblems(problems).build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigValue that = (ConfigValue) o;
        return configSourceOrdinal == that.configSourceOrdinal &&
                configSourcePosition == that.configSourcePosition &&
                name.equals(that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(rawValue, that.rawValue) &&
                Objects.equals(profile, that.profile) &&
                Objects.equals(configSourceName, that.configSourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, rawValue, profile, configSourceName, configSourceOrdinal, configSourcePosition);
    }

    @Override
    public String toString() {
        return "ConfigValue{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", rawValue='" + rawValue + '\'' +
                ", profile='" + profile + '\'' +
                ", configSourceName='" + configSourceName + '\'' +
                ", configSourceOrdinal=" + configSourceOrdinal +
                ", configSourcePosition=" + configSourcePosition +
                ", lineNumber=" + lineNumber +
                '}';
    }

    public ConfigValueBuilder from() {
        return new ConfigValueBuilder()
                .withName(name)
                .withValue(value)
                .withRawValue(rawValue)
                .withProfile(profile)
                .withConfigSourceName(configSourceName)
                .withConfigSourceOrdinal(configSourceOrdinal)
                .withConfigSourcePosition(configSourcePosition)
                .withLineNumber(lineNumber)
                .withProblems(problems);
    }

    public static ConfigValueBuilder builder() {
        return new ConfigValueBuilder();
    }

    public static class ConfigValueBuilder {
        private String name;
        private String value;
        private String rawValue;
        private String profile;
        private String configSourceName;
        private int configSourceOrdinal;
        private int configSourcePosition;
        private int lineNumber = -1;
        private List<Problem> problems;

        public ConfigValueBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public ConfigValueBuilder withValue(final String value) {
            this.value = value;
            return this;
        }

        public ConfigValueBuilder withRawValue(final String rawValue) {
            this.rawValue = rawValue;
            return this;
        }

        public ConfigValueBuilder withProfile(final String profile) {
            this.profile = profile;
            return this;
        }

        public ConfigValueBuilder withConfigSourceName(final String configSourceName) {
            this.configSourceName = configSourceName;
            return this;
        }

        public ConfigValueBuilder withConfigSourceOrdinal(final int configSourceOrdinal) {
            this.configSourceOrdinal = configSourceOrdinal;
            return this;
        }

        public ConfigValueBuilder withConfigSourcePosition(final int configSourcePosition) {
            this.configSourcePosition = configSourcePosition;
            return this;
        }

        public ConfigValueBuilder withLineNumber(final int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public ConfigValueBuilder noProblems() {
            this.problems = emptyList();
            return this;
        }

        public ConfigValueBuilder withProblems(final List<Problem> problems) {
            if (problems != null) {
                if (this.problems == null) {
                    this.problems = new ArrayList<>();
                }
                this.problems.addAll(problems);
            }
            return this;
        }

        public ConfigValueBuilder addProblem(final Problem problem) {
            if (this.problems == null) {
                this.problems = new ArrayList<>();
            }
            this.problems.add(problem);
            return this;
        }

        public ConfigValue build() {
            if (problems != null && !problems.isEmpty()) {
                this.value = null;
            }
            return new ConfigValue(this);
        }
    }

    public static final Comparator<ConfigValue> CONFIG_SOURCE_COMPARATOR = new Comparator<ConfigValue>() {
        @Override
        public int compare(ConfigValue original, ConfigValue candidate) {
            int result = Integer.compare(original.configSourceOrdinal, candidate.configSourceOrdinal);
            if (result != 0) {
                return result;
            }
            result = Integer.compare(original.configSourcePosition, candidate.configSourcePosition) * -1;
            if (result != 0) {
                return result;
            }
            // If both properties are profiled, prioritize the one with the most specific profile.
            if (original.getName().charAt(0) == '%' && candidate.getName().charAt(0) == '%') {
                List<String> originalProfiles = convertProfile(
                        new NameIterator(original.getName()).getNextSegment().substring(1));
                List<String> candidateProfiles = convertProfile(
                        new NameIterator(candidate.getName()).getNextSegment().substring(1));
                return Integer.compare(originalProfiles.size(), candidateProfiles.size()) * -1;
            }
            return result;
        }
    };
}
