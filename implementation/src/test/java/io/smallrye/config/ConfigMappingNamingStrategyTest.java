package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;

public class ConfigMappingNamingStrategyTest {
    @Test
    void namingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerVerbatimNamingStrategy.class, "server")
                .withMapping(ServerSnakeNamingStrategy.class, "server")
                .withSources(config("server.theHost", "localhost", "server.thePort", "8080", "server.log.enabled", "true"))
                .withSources(config("server.the_host", "127.0.0.1", "server.the_port", "80", "server.log.enabled", "true"))
                .build();

        ServerVerbatimNamingStrategy verbatim = config.getConfigMapping(ServerVerbatimNamingStrategy.class, "server");
        assertNotNull(verbatim);
        assertEquals("localhost", verbatim.theHost());
        assertEquals(8080, Integer.valueOf(verbatim.thePort()));
        assertTrue(verbatim.log().enabled());

        ServerSnakeNamingStrategy snake = config.getConfigMapping(ServerSnakeNamingStrategy.class, "server");
        assertNotNull(snake);
        assertEquals("127.0.0.1", snake.theHost());
        assertEquals(80, Integer.valueOf(snake.thePort()));
        assertTrue(snake.log().enabled());
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    public interface ServerVerbatimNamingStrategy {
        String theHost();

        int thePort();

        Log log();

        interface Log {
            boolean enabled();
        }
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    public interface ServerSnakeNamingStrategy {
        String theHost();

        int thePort();

        Log log();

        interface Log {
            boolean enabled();
        }
    }

    @Test
    void composedNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerComposedSnakeNaming.class, "server")
                .withMapping(ServerComposedVerbatimNaming.class, "server")
                .withMapping(ServerComposedKebabNaming.class, "server")
                .withSources(config("server.the_host", "localhost", "server.the_port", "8080"))
                .withSources(config("server.the_log.is_enabled", "true", "server.the_log.log_appenders[0].log_name", "log"))
                .withSources(config("server.theHost", "localhost", "server.thePort", "8080"))
                .withSources(config("server.log.isEnabled", "true", "server.log.logAppenders[0].logName", "log"))
                .withSources(config("server.oLog.isEnabled", "false", "server.oLog.logAppenders[0].logName", "oLog"))
                .withSources(config("server.the-host", "localhost", "server.the-port", "8080"))
                .withSources(config("server.the-log.is-enabled", "true", "server.the-log.log-appenders[0].log-name", "log"))
                .build();

        ServerComposedSnakeNaming snake = config.getConfigMapping(ServerComposedSnakeNaming.class);
        assertNotNull(snake);
        assertEquals("localhost", snake.theHost());
        assertEquals(8080, Integer.valueOf(snake.thePort()));
        assertTrue(snake.theLog().isEnabled());
        assertEquals("log", snake.theLog().logAppenders().get(0).logName());

        ServerComposedVerbatimNaming verbatim = config.getConfigMapping(ServerComposedVerbatimNaming.class);
        assertNotNull(verbatim);
        assertEquals("localhost", verbatim.theHost());
        assertEquals(8080, Integer.valueOf(verbatim.thePort()));
        assertTrue(verbatim.theLog().isEnabled());
        assertEquals("log", verbatim.theLog().logAppenders().get(0).logName());
        assertTrue(verbatim.optionalLog().isPresent());
        assertEquals("oLog", verbatim.optionalLog().get().logAppenders().get(0).logName());

        ServerComposedKebabNaming kebab = config.getConfigMapping(ServerComposedKebabNaming.class);
        assertNotNull(kebab);
        assertEquals("localhost", kebab.theHost());
        assertEquals(8080, Integer.valueOf(kebab.thePort()));
        assertTrue(kebab.theLog().isEnabled());
        assertEquals("log", kebab.theLog().logAppenders().get(0).logName());
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    public interface ServerComposedSnakeNaming {
        String theHost();

        int thePort();

        LogInheritedNaming theLog();
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    public interface ServerComposedVerbatimNaming {
        String theHost();

        int thePort();

        @WithName("log")
        LogInheritedNaming theLog();

        @WithName("oLog")
        Optional<LogInheritedNaming> optionalLog();
    }

    @ConfigMapping(prefix = "server")
    public interface ServerComposedKebabNaming {
        String theHost();

        int thePort();

        LogInheritedNaming theLog();
    }

    public interface LogInheritedNaming {
        boolean isEnabled();

        List<Appender> logAppenders();

        interface Appender {
            String logName();
        }
    }

    @Test
    void interfaceAndClass() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ConfigMappingNamingKebab.class, "server")
                .withMapping(ConfigPropertiesNamingVerbatim.class, "server")
                .withSources(config("server.theHost", "localhost"))
                .withSources(config("server.the-host", "localhost", "server.form.login-page", "login"))
                .build();

        ConfigPropertiesNamingVerbatim configProperties = config.getConfigMapping(ConfigPropertiesNamingVerbatim.class);
        assertEquals("localhost", configProperties.theHost);

        ConfigMappingNamingKebab configMapping = config.getConfigMapping(ConfigMappingNamingKebab.class);
        assertEquals("localhost", configMapping.theHost());
        assertEquals("login", configMapping.form().get("form").loginPage());
    }

    @ConfigProperties(prefix = "server")
    public static class ConfigPropertiesNamingVerbatim {
        String theHost;
    }

    @ConfigMapping(prefix = "server")
    public interface ConfigMappingNamingKebab {
        String theHost();

        @WithParentName
        Map<String, Form> form();

        interface Form {
            String loginPage();
        }
    }

    // From https://github.com/quarkusio/quarkus/issues/21407
    @ConfigMapping(prefix = "bugs", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    public interface NamingStrategyVerbatimOptionalGroup {
        @WithParentName
        Map<String, ClientConfiguration> bugs();

        interface ClientConfiguration {
            boolean hereVerbatimWorks();

            Optional<Properties> properties();

            Optional<OverrideNamingStrategyProperties> override();
        }

        interface Properties {
            String feed();

            String customerId();

            String scriptSelector();
        }

        @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
        interface OverrideNamingStrategyProperties {
            String feed();

            String customerId();

            String scriptSelector();
        }
    }

    @Test
    void namingStrategyVerbatimOptionalGroup() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NamingStrategyVerbatimOptionalGroup.class, "bugs")
                .withSources(config("bugs.KEY1.hereVerbatimWorks", "true",
                        "bugs.KEY1.properties.feed", "100103",
                        "bugs.KEY1.properties.customerId", "36936471",
                        "bugs.KEY1.properties.scriptSelector", "RoadRunner_Task1_AAR01",
                        "bugs.KEY1.override.feed", "100103",
                        "bugs.KEY1.override.customer-id", "36936471",
                        "bugs.KEY1.override.script-selector", "RoadRunner_Task1_AAR01"))
                .build();

        NamingStrategyVerbatimOptionalGroup mapping = config.getConfigMapping(NamingStrategyVerbatimOptionalGroup.class);

        assertTrue(mapping.bugs().get("KEY1").hereVerbatimWorks());
        assertTrue(mapping.bugs().get("KEY1").properties().isPresent());
        assertEquals("100103", mapping.bugs().get("KEY1").properties().get().feed());
        assertEquals("36936471", mapping.bugs().get("KEY1").properties().get().customerId());
        assertEquals("RoadRunner_Task1_AAR01", mapping.bugs().get("KEY1").properties().get().scriptSelector());
        assertTrue(mapping.bugs().get("KEY1").override().isPresent());
        assertEquals("100103", mapping.bugs().get("KEY1").override().get().feed());
        assertEquals("36936471", mapping.bugs().get("KEY1").override().get().customerId());
        assertEquals("RoadRunner_Task1_AAR01", mapping.bugs().get("KEY1").override().get().scriptSelector());
    }
}
