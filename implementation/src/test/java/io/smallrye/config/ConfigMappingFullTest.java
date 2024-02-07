package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

public class ConfigMappingFullTest {
    @Test
    void ambiguousUnnamedKeysDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "datasource.postgresql.jdbc.url", "value",
                        "datasource.postgresql.password", "value"))
                .withMapping(DataSourcesJdbcRuntimeConfig.class)
                .withMapping(DataSourcesJdbcBuildTimeConfig.class)
                .withMapping(DataSourcesRuntimeConfig.class)
                .withMapping(DataSourcesBuildTimeConfig.class)
                .build();

        assertTrue(
                config.getConfigMapping(DataSourcesRuntimeConfig.class).dataSources().get("postgresql").password().isPresent());

        config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "datasource.postgresql.jdbc.url", "value",
                        "datasource.postgresql.password", "value"))
                .withMapping(DataSourcesJdbcBuildTimeConfig.class)
                .withMapping(DataSourcesJdbcRuntimeConfig.class)
                .withMapping(DataSourcesRuntimeConfig.class)
                .withMapping(DataSourcesBuildTimeConfig.class)
                .build();

        assertTrue(
                config.getConfigMapping(DataSourcesRuntimeConfig.class).dataSources().get("postgresql").password().isPresent());

        config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "datasource.postgresql.jdbc.url", "value",
                        "datasource.postgresql.password", "value"))
                .withMapping(DataSourcesJdbcBuildTimeConfig.class)
                .withMapping(DataSourcesJdbcRuntimeConfig.class)
                .withMapping(DataSourcesBuildTimeConfig.class)
                .withMapping(DataSourcesRuntimeConfig.class)
                .build();

        assertTrue(
                config.getConfigMapping(DataSourcesRuntimeConfig.class).dataSources().get("postgresql").password().isPresent());

        config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "datasource.postgresql.jdbc.url", "value",
                        "datasource.postgresql.password", "value"))
                .withMapping(DataSourcesRuntimeConfig.class)
                .withMapping(DataSourcesJdbcBuildTimeConfig.class)
                .withMapping(DataSourcesJdbcRuntimeConfig.class)
                .withMapping(DataSourcesBuildTimeConfig.class)
                .build();

        assertTrue(
                config.getConfigMapping(DataSourcesRuntimeConfig.class).dataSources().get("postgresql").password().isPresent());
    }

    @ConfigMapping(prefix = "datasource")
    interface DataSourcesRuntimeConfig {
        @WithParentName
        @WithDefaults
        @WithUnnamedKey("<default>")
        Map<String, DataSourceRuntimeConfig> dataSources();

        interface DataSourceRuntimeConfig {
            @WithDefault("true")
            boolean active();

            Optional<String> username();

            Optional<String> password();

            Optional<String> credentialsProvider();

            Optional<String> credentialsProviderName();
        }
    }

    @ConfigMapping(prefix = "datasource")
    interface DataSourcesJdbcRuntimeConfig {
        DataSourceJdbcRuntimeConfig jdbc();

        @WithParentName
        @WithDefaults
        Map<String, DataSourceJdbcOuterNamedRuntimeConfig> namedDataSources();

        interface DataSourceJdbcOuterNamedRuntimeConfig {
            DataSourceJdbcRuntimeConfig jdbc();
        }

        interface DataSourceJdbcRuntimeConfig {
            Optional<String> url();

            OptionalInt initialSize();

            @WithDefault("0")
            int minSize();

            @WithDefault("20")
            int maxSize();

            @WithDefault("2M")
            String backgroundValidationInterval();

            Optional<String> foregroundValidationInterval();

            @WithDefault("5S")
            Optional<String> acquisitionTimeout();

            Optional<String> leakDetectionInterval();

            @WithDefault("5M")
            String idleRemovalInterval();

            Optional<String> maxLifetime();

            @WithDefault("false")
            boolean extendedLeakReport();

            @WithDefault("false")
            boolean flushOnClose();

            @WithDefault("true")
            boolean detectStatementLeaks();

            Optional<String> newConnectionSql();

            Optional<String> validationQuerySql();

            @WithDefault("true")
            boolean poolingEnabled();

            Map<String, String> additionalJdbcProperties();

            @WithName("telemetry.enabled")
            Optional<Boolean> telemetry();
        }
    }

    @ConfigMapping(prefix = "datasource")
    interface DataSourcesBuildTimeConfig {
        @WithParentName
        @WithDefaults
        @WithUnnamedKey("<default>")
        Map<String, DataSourceBuildTimeConfig> dataSources();

        @WithName("health.enabled")
        @WithDefault("true")
        boolean healthEnabled();

        @WithName("metrics.enabled")
        @WithDefault("false")
        boolean metricsEnabled();

        @Deprecated
        Optional<String> url();

        @Deprecated
        Optional<String> driver();

        interface DataSourceBuildTimeConfig {
            Optional<String> dbKind();

            Optional<String> dbVersion();

            DevServicesBuildTimeConfig devservices();

            @WithDefault("false")
            boolean healthExclude();

            interface DevServicesBuildTimeConfig {
                Optional<Boolean> enabled();

                Optional<String> imageName();

                Map<String, String> containerEnv();

                Map<String, String> containerProperties();

                Map<String, String> properties();

                OptionalInt port();

                Optional<String> command();

                Optional<String> dbName();

                Optional<String> username();

                Optional<String> password();

                Optional<String> initScriptPath();

                Map<String, String> volumes();

                @WithDefault("true")
                boolean reuse();
            }
        }
    }

    @ConfigMapping(prefix = "datasource")
    interface DataSourcesJdbcBuildTimeConfig {
        @WithParentName
        @WithDefaults
        @WithUnnamedKey("<ddefault>")
        Map<String, DataSourceJdbcOuterNamedBuildTimeConfig> dataSources();

        interface DataSourceJdbcOuterNamedBuildTimeConfig {
            DataSourceJdbcBuildTimeConfig jdbc();

            interface DataSourceJdbcBuildTimeConfig {
                @WithParentName
                @WithDefault("true")
                boolean enabled();

                Optional<String> driver();

                Optional<Boolean> enableMetrics();

                @WithDefault("false")
                boolean tracing();

                @WithDefault("false")
                boolean telemetry();
            }
        }
    }
}
