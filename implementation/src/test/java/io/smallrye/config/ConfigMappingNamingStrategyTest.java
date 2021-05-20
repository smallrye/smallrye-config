package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
                .withValidateUnknown(false)
                .withMapping(ServerComposedSnakeNaming.class, "server")
                .withMapping(ServerComposedVerbatimNaming.class, "server")
                .withMapping(ServerComposedKebabNaming.class, "server")
                .withSources(config("server.the_host", "localhost", "server.the_port", "8080"))
                .withSources(config("server.log.is_enabled", "true", "server.log.log_appenders[0].log_name", "log"))
                .withSources(config("server.theHost", "localhost", "server.thePort", "8080"))
                .withSources(config("server.log.isEnabled", "true", "server.log.logAppenders[0].logName", "log"))
                .withSources(config("server.the-host", "localhost", "server.the-port", "8080"))
                .withSources(config("server.log.is-enabled", "true", "server.log.log-appenders[0].log-name", "log"))
                .build();

        ServerComposedSnakeNaming snake = config.getConfigMapping(ServerComposedSnakeNaming.class);
        assertNotNull(snake);
        assertEquals("localhost", snake.theHost());
        assertEquals(8080, Integer.valueOf(snake.thePort()));
        assertTrue(snake.log().isEnabled());
        assertEquals("log", snake.log().logAppenders().get(0).logName());

        ServerComposedVerbatimNaming verbatim = config.getConfigMapping(ServerComposedVerbatimNaming.class);
        assertNotNull(verbatim);
        assertEquals("localhost", verbatim.theHost());
        assertEquals(8080, Integer.valueOf(verbatim.thePort()));
        assertTrue(verbatim.log().isEnabled());
        assertEquals("log", verbatim.log().logAppenders().get(0).logName());

        ServerComposedKebabNaming kebab = config.getConfigMapping(ServerComposedKebabNaming.class);
        assertNotNull(kebab);
        assertEquals("localhost", kebab.theHost());
        assertEquals(8080, Integer.valueOf(kebab.thePort()));
        assertTrue(kebab.log().isEnabled());
        assertEquals("log", kebab.log().logAppenders().get(0).logName());
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    public interface ServerComposedSnakeNaming {
        String theHost();

        int thePort();

        LogInheritedNaming log();
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    public interface ServerComposedVerbatimNaming {
        String theHost();

        int thePort();

        LogInheritedNaming log();
    }

    @ConfigMapping(prefix = "server")
    public interface ServerComposedKebabNaming {
        String theHost();

        int thePort();

        LogInheritedNaming log();
    }

    public interface LogInheritedNaming {
        boolean isEnabled();

        List<Appender> logAppenders();

        interface Appender {
            String logName();
        }
    }
}
