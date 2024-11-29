package io.smallrye.config.source.yaml;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class YamlConfigSourceTest {
    @Test
    void profiles() {
        String yaml = "---\n" +
                "foo:\n" +
                "  bar:\n" +
                "    default\n" +
                "---\n" +
                "\"%dev\":\n" +
                "  foo:\n" +
                "    bar:\n" +
                "      dev\n" +
                "---\n" +
                "\"%prod\":\n" +
                "  foo:\n" +
                "    bar:\n" +
                "      prod\n";

        YamlConfigSource source = new YamlConfigSource("yaml", yaml);
        assertEquals("default", source.getValue("foo.bar"));
        assertEquals("dev", source.getValue("%dev.foo.bar"));
        assertEquals("prod", source.getValue("%prod.foo.bar"));
    }

    @Test
    void list() {
        String yaml = "quarkus:\n" +
                "  http:\n" +
                "    ssl:\n" +
                "      protocols:\n" +
                "        - TLSv1.2\n" +
                "        - TLSv1.3";

        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(new YamlConfigSource("Yaml", yaml)).build();
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
    void indentSpaces() {
        String yaml;
        SmallRyeConfig config;

        // 1
        yaml = "greeting:\n" +
                " message: hello\n" +
                " name: smallrye";

        config = new SmallRyeConfigBuilder().withSources(new YamlConfigSource("Yaml", yaml)).build();
        assertEquals("hello", config.getRawValue("greeting.message"));
        assertEquals("smallrye", config.getRawValue("greeting.name"));

        // 2
        yaml = "greeting:\n" +
                "  message: hello\n" +
                "  name: smallrye";

        config = new SmallRyeConfigBuilder().withSources(new YamlConfigSource("Yaml", yaml)).build();
        assertEquals("hello", config.getRawValue("greeting.message"));
        assertEquals("smallrye", config.getRawValue("greeting.name"));

        // 4
        yaml = "greeting:\n" +
                "    message: hello\n" +
                "    name: smallrye";

        config = new SmallRyeConfigBuilder().withSources(new YamlConfigSource("Yaml", yaml)).build();
        assertEquals("hello", config.getRawValue("greeting.message"));
        assertEquals("smallrye", config.getRawValue("greeting.name"));

        // 8
        yaml = "greeting:\n" +
                "        message: hello\n" +
                "        name: smallrye";

        config = new SmallRyeConfigBuilder().withSources(new YamlConfigSource("Yaml", yaml)).build();
        assertEquals("hello", config.getRawValue("greeting.message"));
        assertEquals("smallrye", config.getRawValue("greeting.name"));

        // 11
        yaml = "greeting:\n" +
                "           message: hello\n" +
                "           name: smallrye";

        config = new SmallRyeConfigBuilder().withSources(new YamlConfigSource("Yaml", yaml)).build();
        assertEquals("hello", config.getRawValue("greeting.message"));
        assertEquals("smallrye", config.getRawValue("greeting.name"));
    }

    @Test
    void propertyNames() {
        String yaml = "quarkus:\n" +
                "  http:\n" +
                "    port: 8081\n" +
                "    ssl-port: 2443\n" +
                "    cors:\n" +
                "      ~: true\n" +
                "      access-control-max-age: 24H\n" +
                "      exposed-headers: \"SOME-HEADER\"\n" +
                "      methods: GET,PUT,POST,DELETE,OPTIONS\n" +
                "    ssl:\n" +
                "      protocols:\n" +
                "        - TLSv1.2\n" +
                "        - TLSv1.3\n" +
                "      cipher-suites:\n" +
                "        - TLS_AES_128_GCM_SHA256\n" +
                "        - TLS_AES_256_GCM_SHA384\n" +
                "        - TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384\n" +
                "        - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384\n" +
                "        - TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256\n" +
                "        - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256\n" +
                "  swagger-ui:\n" +
                "    always-include: true\n" +
                "\n" +
                "  jib:\n" +
                "    jvm-arguments:\n" +
                "      - \"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005\"\n" +
                "      - \"-Dquarkus.http.host=0.0.0.0\"\n" +
                "      - \"-Djava.util.logging.manager=org.jboss.logmanager.LogManager\"\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        List<String> propertyNames = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toList());

        assertTrue(propertyNames.contains("quarkus.http.port"));
        assertTrue(propertyNames.contains("quarkus.http.ssl-port"));
        assertTrue(propertyNames.contains("quarkus.http.ssl.protocols"));
        assertTrue(propertyNames.contains("quarkus.http.ssl.protocols[0]"));
        assertNotNull(config.getRawValue("quarkus.http.ssl.protocols[0]"));
    }

    @Test
    void quotedProperties() {
        String yaml = "quarkus:\n" +
                "  log:\n" +
                "    category:\n" +
                "      \"liquibase.changelog.ChangeSet\":\n" +
                "        level: INFO\n" +
                "      \"liquibase\":\n" +
                "        level: WARN\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        List<String> propertyNames = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toList());

        assertTrue(propertyNames.contains("quarkus.log.category.liquibase.level"));
        assertTrue(propertyNames.contains("quarkus.log.category.\"liquibase.changelog.ChangeSet\".level"));
        assertNotNull(config.getRawValue("quarkus.log.category.\"liquibase.changelog.ChangeSet\".level"));
    }

    @Test
    void commas() {
        String yaml = "quarkus:\n" +
                "  http:\n" +
                "    port: 8081\n" +
                "    ssl-port: 2443\n" +
                "    cors:\n" +
                "      ~: true\n" +
                "      access-control-max-age: 24H\n" +
                "      exposed-headers: \"SOME-HEADER\"\n" +
                "      methods: GET,PUT,POST,DELETE,OPTIONS\n" +
                "    ssl:\n" +
                "      protocols:\n" +
                "        - TLSv1.2\n" +
                "        - TLSv1.3\n" +
                "      cipher-suites:\n" +
                "        - TLS_AES_128_GCM_SHA256\n" +
                "        - TLS_AES_256_GCM_SHA384\n" +
                "        - TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384\n" +
                "        - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384\n" +
                "        - TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256\n" +
                "        - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256\n" +
                "  swagger-ui:\n" +
                "    always-include: true\n" +
                "\n" +
                "  jib:\n" +
                "    jvm-arguments:\n" +
                "      - \"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005\"\n" +
                "      - \"-Dquarkus.http.host=0.0.0.0\"\n" +
                "      - \"-Djava.util.logging.manager=org.jboss.logmanager.LogManager\"\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        String[] values = config.getValue("quarkus.jib.jvm-arguments", String[].class);
        assertEquals(3, values.length);
        assertEquals("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", values[0]);
    }

    @Test
    void intKeys() {
        String yaml = "storefront:\n" +
                "  path:\n" +
                "    appConfig:\n" +
                "      1: /storefront/appConfig/*\n" +
                "      2: /storefront/storefront/appConfig/*\n" +
                "    training:\n" +
                "      1: /storefront/training/*\n" +
                "      2: /storefront/storefront/training/*\n";

        try {
            new SmallRyeConfigBuilder()
                    .withSources(new YamlConfigSource("yaml", yaml))
                    .build();
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void mapping() {
        String yaml = "admin:\n" +
                "  users:\n" +
                "    -\n" +
                "      email: \"joe@gmail.com\"\n" +
                "      username: \"joe\"\n" +
                "      password: \"123456\"\n" +
                "      roles:\n" +
                "        - \"Moderator\"\n" +
                "        - \"Admin\"\n" +
                "    -\n" +
                "      email: \"jack@gmail.com\"\n" +
                "      username: \"jack\"\n" +
                "      password: \"654321\"\n" +
                "      roles:\n" +
                "        - \"Moderator\"\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
                .withMapping(UsersMapping.class)
                .build();

        UsersMapping usersMapping = config.getConfigMapping(UsersMapping.class);
        assertEquals(2, usersMapping.users().size());
        assertEquals(usersMapping.users().get(0).email(), "joe@gmail.com");
        assertEquals(usersMapping.users().get(0).username(), "joe");
        assertEquals(usersMapping.users().get(0).password(), "123456");
        assertEquals(usersMapping.users().get(0).roles(), Stream.of("Moderator", "Admin").collect(toList()));
    }

    @Test
    void mappingCollections() {
        String yaml = "application:\n" +
                "  environments:\n" +
                "    - name: dev\n" +
                "      services:\n" +
                "        - name: batch\n" +
                "        - name: rest\n" +
                "    - name: prod\n" +
                "      services:\n" +
                "        - name: web\n" +
                "        - name: batch\n" +
                "        - name: rest\n" +
                "  images:\n" +
                "    - base\n" +
                "    - jdk\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
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
    void optional() {
        String yaml = "---\n" +
                "\"%base\":\n" +
                "  server:\n" +
                "    name: localhost\n" +
                "    config:\n" +
                "      server: localhost\n" +
                "      port: 1143\n" +
                "      user: user\n" +
                "      password: password\n" +
                "      version:\n" +
                "        major: 16\n" +
                "        minor: 0\n" +
                "---\n" +
                "\"%empty\":\n" +
                "  server:\n" +
                "    name: localhost\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
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
                .withSources(new YamlConfigSource("yaml", yaml))
                .withMapping(Server.class)
                .withProfile("empty")
                .build();

        server = config.getConfigMapping(Server.class);
        assertEquals("localhost", server.name());
        assertFalse(server.config().isPresent());
    }

    @Test
    void timestampConverters() {
        String yaml = "date: 2010-10-10\n" +
                "dateTime: 2010-10-10T10:10:10\n" +
                "zonedDateTime: 2020-10-10T10:10:10-05:00";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        assertEquals(LocalDate.of(2010, 10, 10), config.getValue("date", LocalDate.class));
        assertEquals(LocalDateTime.of(2010, 10, 10, 10, 10, 10), config.getValue("dateTime", LocalDateTime.class));
        assertEquals(ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("-5")),
                config.getValue("zonedDateTime", ZonedDateTime.class));
    }

    public static class Users {
        List<User> users;

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(final List<User> users) {
            this.users = users;
        }
    }

    public static class User {
        String email;
        String username;
        String password;
        List<String> roles;

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(final List<String> roles) {
            this.roles = roles;
        }
    }

    static class UserConverter implements Converter<Users> {
        @Override
        public Users convert(final String value) {
            return new Yaml().loadAs(value, Users.class);
        }
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
