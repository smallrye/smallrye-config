package io.smallrye.config.source.toml.test;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.toml.TomlConfigSource;

class TomlConfigSourceTest {
    @Test
    void basicKeyValue() {
        String toml = """
                greeting = "hello"
                name = "smallrye"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        assertEquals("hello", config.getConfigValue("greeting").getValue());
        assertEquals("smallrye", config.getConfigValue("name").getValue());
    }

    @Test
    void nestedTables() {
        String toml = """
                [greeting]
                message = "hello"
                name = "smallrye"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        assertEquals("hello", config.getConfigValue("greeting.message").getValue());
        assertEquals("smallrye", config.getConfigValue("greeting.name").getValue());
    }

    @Test
    void deeplyNestedTables() {
        String toml = """
                [quarkus.http]
                port = 8081
                ssl-port = 2443

                [quarkus.http.cors]
                access-control-max-age = "24H"
                exposed-headers = "SOME-HEADER"
                methods = "GET,PUT,POST,DELETE,OPTIONS"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        assertEquals("8081", config.getConfigValue("quarkus.http.port").getValue());
        assertEquals("2443", config.getConfigValue("quarkus.http.ssl-port").getValue());
        assertEquals("24H", config.getConfigValue("quarkus.http.cors.access-control-max-age").getValue());
    }

    @Test
    void list() {
        String toml = """
                [quarkus.http.ssl]
                protocols = ["TLSv1.2", "TLSv1.3"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        String[] values = config.getValue("quarkus.http.ssl.protocols", String[].class);
        assertEquals(2, values.length);
        assertEquals("TLSv1.2", values[0]);
        assertEquals("TLSv1.3", values[1]);

        List<String> list = config.getValues("quarkus.http.ssl.protocols", String.class, ArrayList::new);
        assertEquals(2, list.size());
        assertEquals("TLSv1.2", list.get(0));
        assertEquals("TLSv1.3", list.get(1));
    }

    @Test
    void propertyNames() {
        String toml = """
                [quarkus.http]
                port = 8081
                ssl-port = 2443

                [quarkus.http.ssl]
                protocols = ["TLSv1.2", "TLSv1.3"]

                [quarkus]
                swagger-ui.always-include = true
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        List<String> propertyNames = StreamSupport.stream(config.getPropertyNames().spliterator(), false).toList();

        assertTrue(propertyNames.contains("quarkus.http.port"));
        assertTrue(propertyNames.contains("quarkus.http.ssl-port"));
        assertTrue(propertyNames.contains("quarkus.http.ssl.protocols[0]"));
        assertNotNull(config.getConfigValue("quarkus.http.ssl.protocols[0]").getValue());
    }

    @Test
    void quotedKeys() {
        String toml = """
                [quarkus.log.category]
                "liquibase.changelog.ChangeSet".level = "INFO"
                "liquibase".level = "WARN"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        List<String> propertyNames = StreamSupport.stream(config.getPropertyNames().spliterator(), false).toList();

        assertTrue(propertyNames.contains("quarkus.log.category.\"liquibase.changelog.ChangeSet\".level"));
        assertTrue(propertyNames.contains("quarkus.log.category.liquibase.level"));
        assertNotNull(config.getConfigValue("quarkus.log.category.\"liquibase.changelog.ChangeSet\".level").getValue());
    }

    @Test
    void profiles() {
        String toml = """
                [foo]
                bar = "default"

                ["%dev".foo]
                bar = "dev"

                ["%prod".foo]
                bar = "prod"
                """;

        TomlConfigSource source = new TomlConfigSource("toml", toml);
        assertEquals("default", source.getValue("foo.bar"));
        assertEquals("dev", source.getValue("%dev.foo.bar"));
        assertEquals("prod", source.getValue("%prod.foo.bar"));
    }

    @Test
    void mapping() {
        String toml = """
                [[admin.users]]
                email = "joe@gmail.com"
                username = "joe"
                password = "123456"
                roles = ["Moderator", "Admin"]

                [[admin.users]]
                email = "jack@gmail.com"
                username = "jack"
                password = "654321"
                roles = ["Moderator"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .withMapping(UsersMapping.class)
                .build();

        UsersMapping usersMapping = config.getConfigMapping(UsersMapping.class);
        assertEquals(2, usersMapping.users().size());
        assertEquals("joe@gmail.com", usersMapping.users().get(0).email());
        assertEquals("joe", usersMapping.users().get(0).username());
        assertEquals("123456", usersMapping.users().get(0).password());
        assertEquals(Stream.of("Moderator", "Admin").collect(toList()), usersMapping.users().get(0).roles());
    }

    @Test
    void mappingCollections() {
        String toml = """
                [[application.environments]]
                name = "dev"

                [[application.environments.services]]
                name = "batch"

                [[application.environments.services]]
                name = "rest"

                [[application.environments]]
                name = "prod"

                [[application.environments.services]]
                name = "web"

                [[application.environments.services]]
                name = "batch"

                [[application.environments.services]]
                name = "rest"

                [application]
                images = ["base", "jdk"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .withMapping(Application.class)
                .build();

        Application application = config.getConfigMapping(Application.class);
        assertEquals(2, application.environments().size());
        assertEquals("dev", application.environments().get(0).name());
        assertEquals(2, application.environments().get(0).services().size());
        assertEquals("batch", application.environments().get(0).services().get(0).name());
        assertEquals("rest", application.environments().get(0).services().get(1).name());
        assertEquals(3, application.environments().get(1).services().size());
        assertEquals("web", application.environments().get(1).services().get(0).name());
        assertEquals("batch", application.environments().get(1).services().get(1).name());
        assertEquals("rest", application.environments().get(1).services().get(2).name());
        assertEquals(2, application.images().size());
        assertEquals("base", application.images().get(0));
        assertEquals("jdk", application.images().get(1));
    }

    @Test
    void integerAndFloatValues() {
        String toml = """
                port = 8080
                ratio = 3.14
                enabled = true
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        assertEquals("8080", config.getConfigValue("port").getValue());
        assertEquals("3.14", config.getConfigValue("ratio").getValue());
        assertEquals("true", config.getConfigValue("enabled").getValue());
    }

    @Test
    void dateValues() {
        String toml = """
                date = 2010-10-10
                dateTime = 2010-10-10T10:10:10
                zonedDateTime = 2020-10-10T10:10:10-05:00
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        assertEquals(LocalDate.of(2010, 10, 10), config.getValue("date", LocalDate.class));
        assertEquals(LocalDateTime.of(2010, 10, 10, 10, 10, 10), config.getValue("dateTime", LocalDateTime.class));
        assertEquals(ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("-5")),
                config.getValue("zonedDateTime", ZonedDateTime.class));
    }

    @Test
    void optional() {
        String toml = """
                ["%base".server]
                name = "localhost"

                ["%base".server.config]
                server = "localhost"
                port = 1143
                user = "user"
                password = "password"

                ["%base".server.config.version]
                major = 16
                minor = 0

                ["%empty".server]
                name = "localhost"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .withMapping(Server.class)
                .withProfile("base")
                .build();

        Server server = config.getConfigMapping(Server.class);
        assertEquals("localhost", server.name());
        assertTrue(server.config().isPresent());
        assertEquals("localhost", server.config().get().server());
        assertEquals(1143, server.config().get().port());
        assertEquals("user", server.config().get().user());
        assertEquals("password", server.config().get().password());
        assertEquals(16, server.config().get().version().major());
        assertEquals(0, server.config().get().version().minor());

        config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .withMapping(Server.class)
                .withProfile("empty")
                .build();

        server = config.getConfigMapping(Server.class);
        assertEquals("localhost", server.name());
        assertFalse(server.config().isPresent());
    }

    @Test
    void commasInValues() {
        String toml = """
                [quarkus.jib]
                jvm-arguments = ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-Dquarkus.http.host=0.0.0.0", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .build();

        String[] values = config.getValue("quarkus.jib.jvm-arguments", String[].class);
        assertEquals(3, values.length);
        assertEquals("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", values[0]);
    }

    @ConfigMapping(prefix = "admin")
    interface UsersMapping {
        List<UserMapping> users();
    }

    public interface UserMapping {
        String email();

        String username();

        String password();

        List<String> roles();
    }

    @ConfigMapping(prefix = "application")
    interface Application {
        List<Environment> environments();

        List<String> images();

        interface Environment {
            String name();

            List<Service> services();

            interface Service {
                String name();
            }
        }
    }

    @ConfigMapping(prefix = "server")
    interface Server {
        String name();

        Optional<ServerConfig> config();

        interface ServerConfig {
            String server();

            int port();

            String user();

            String password();

            Version version();

            interface Version {
                short major();

                short minor();
            }
        }
    }
}
