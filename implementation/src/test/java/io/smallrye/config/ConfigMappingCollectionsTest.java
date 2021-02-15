package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappingCollectionsTest.ServerCollectionsSet.Environment;
import io.smallrye.config.ConfigMappingCollectionsTest.ServerCollectionsSet.Environment.App;

public class ConfigMappingCollectionsTest {
    @ConfigMapping(prefix = "server")
    public interface ServerCollectionSimple {
        List<String> environments();
    }

    @Test
    void mappingCollectionsSimple() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionSimple.class, "server")
                .withSources(config("server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        List<String> environments = config.getValues("server.environments", String.class);
        assertEquals(3, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("qa", environments.get(1));
        assertEquals("prod", environments.get(2));

        ServerCollectionSimple configMapping = config.getConfigMapping(ServerCollectionSimple.class);
        assertEquals(3, configMapping.environments().size());
        assertEquals("dev", configMapping.environments().get(0));
        assertEquals("qa", configMapping.environments().get(1));
        assertEquals("prod", configMapping.environments().get(2));
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollections {
        List<Environment> environments();

        interface Environment {
            String name();

            List<App> apps();

            interface App {
                String name();

                List<String> services();

                Optional<List<String>> databases();
            }
        }
    }

    @Test
    void mappingCollections() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollections.class, "server")
                .withSources(config(
                        "server.environments[0].name", "dev",
                        "server.environments[0].apps[0].name", "rest",
                        "server.environments[0].apps[0].services", "a,b,c",
                        "server.environments[0].apps[0].databases", "pg,h2",
                        "server.environments[0].apps[1].name", "batch",
                        "server.environments[0].apps[1].services", "a,b,c",
                        "server.environments[1].name", "prod",
                        "server.environments[1].apps[0].name", "web",
                        "server.environments[1].apps[0].services", "a,b,c",
                        "server.environments[1].apps[1].name", "rest",
                        "server.environments[1].apps[1].services", "a,b,c",
                        "server.environments[1].apps[2].name", "batch",
                        "server.environments[1].apps[2].services", "a,b,c"))
                .build();
        ServerCollections server = config.getConfigMapping(ServerCollections.class);

        assertEquals(2, server.environments().size());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(Stream.of("a", "b", "c").collect(toList()), server.environments().get(0).apps().get(0).services());
        assertTrue(server.environments().get(0).apps().get(0).databases().isPresent());
        assertEquals(Stream.of("pg", "h2").collect(toList()), server.environments().get(0).apps().get(0).databases().get());
        assertEquals(2, server.environments().get(0).apps().size());
        assertEquals("rest", server.environments().get(0).apps().get(0).name());
        assertEquals("batch", server.environments().get(0).apps().get(1).name());
        assertEquals("prod", server.environments().get(1).name());
        assertEquals(3, server.environments().get(1).apps().size());
        assertEquals("web", server.environments().get(1).apps().get(0).name());
        assertEquals("rest", server.environments().get(1).apps().get(1).name());
        assertEquals("batch", server.environments().get(1).apps().get(2).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionName {
        @WithName("envs")
        List<Environment> environments();

        interface Environment {
            String name();

            @WithName("apps")
            List<App> applications();

            interface App {
                String name();
            }
        }
    }

    @Test
    void mappingCollectionsWithName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionName.class, "server")
                .withSources(config("server.envs[0].name", "dev",
                        "server.envs[0].apps[0].name", "rest",
                        "server.envs[0].apps[1].name", "batch",
                        "server.envs[1].name", "prod",
                        "server.envs[1].apps[0].name", "web",
                        "server.envs[1].apps[1].name", "rest",
                        "server.envs[1].apps[2].name", "batch"))
                .build();
        ServerCollectionName server = config.getConfigMapping(ServerCollectionName.class);

        assertEquals(2, server.environments().size());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(2, server.environments().get(0).applications().size());
        assertEquals("rest", server.environments().get(0).applications().get(0).name());
        assertEquals("batch", server.environments().get(0).applications().get(1).name());
        assertEquals("prod", server.environments().get(1).name());
        assertEquals(3, server.environments().get(1).applications().size());
        assertEquals("web", server.environments().get(1).applications().get(0).name());
        assertEquals("rest", server.environments().get(1).applications().get(1).name());
        assertEquals("batch", server.environments().get(1).applications().get(2).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsDefaults {
        List<Environment> environments();

        @WithDefault("80,443")
        List<Integer> ports();

        interface Environment {
            String name();

            @WithDefault("web,rest")
            List<String> apps();
        }
    }

    @Test
    void mappingCollectionsWithDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsDefaults.class, "server")
                .withSources(config("server.environments[0].name", "dev"))
                .build();

        ServerCollectionsDefaults server = config.getConfigMapping(ServerCollectionsDefaults.class);

        assertEquals(1, server.environments().size());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(Stream.of("web", "rest").collect(toList()), server.environments().get(0).apps());

        assertEquals(2, server.ports().size());
        assertEquals(Stream.of(80, 443).collect(toList()), server.ports());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsOptionals {
        Optional<Environment> notRequired();

        Optional<Environment> required();

        Optional<List<Environment>> notRequiredEnvs();

        Optional<List<Environment>> requiredEnvs();

        interface Environment {
            String name();

            Optional<List<String>> notRequiredServices();

            Optional<List<String>> requiredServices();

            Optional<List<String>> indexed();
        }
    }

    @Test
    void mappingCollectionsOptionals() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsOptionals.class, "server")
                .withSources(config("server.required.name", "dev",
                        "server.required.required-services", "rest,db",
                        "server.required.indexed[0]", "rest",
                        "server.required.indexed[1]", "db",
                        "server.required-envs[0].name", "dev",
                        "server.required-envs[1].name", "prod"))
                .build();

        ServerCollectionsOptionals server = config.getConfigMapping(ServerCollectionsOptionals.class);

        assertFalse(server.notRequired().isPresent());
        assertTrue(server.required().isPresent());
        assertEquals("dev", server.required().get().name());
        assertFalse(server.required().get().notRequiredServices().isPresent());
        assertTrue(server.required().get().requiredServices().isPresent());
        assertEquals("rest", server.required().get().requiredServices().get().get(0));
        assertEquals("db", server.required().get().requiredServices().get().get(1));
        assertTrue(server.required().get().indexed().isPresent());
        assertEquals("rest", server.required().get().indexed().get().get(0));
        assertEquals("db", server.required().get().indexed().get().get(1));

        assertFalse(server.notRequiredEnvs().isPresent());
        assertTrue(server.requiredEnvs().isPresent());
        assertEquals("dev", server.requiredEnvs().get().get(0).name());
        assertEquals("prod", server.requiredEnvs().get().get(1).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsConverters {
        List<Enviroment> envs();

        interface Enviroment {
            @WithConverter(HostConverter.class)
            List<Host> hosts();
        }
    }

    static class Host {
        final String name;

        Host(final String name) {
            this.name = name;
        }
    }

    static class HostConverter implements Converter<Host> {
        public HostConverter() {
        }

        @Override
        public Host convert(final String value) throws IllegalArgumentException, NullPointerException {
            return new Host(value);
        }
    }

    @Test
    void mappingCollectionsConverters() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsConverters.class, "server")
                .withSources(config("server.envs[0].hosts[0]", "localhost",
                        "server.envs[0].hosts[1]", "127.0.0.1"))
                .build();

        ServerCollectionsConverters server = config.getConfigMapping(ServerCollectionsConverters.class);

        assertEquals(1, server.envs().size());
        assertEquals(2, server.envs().get(0).hosts().size());
        assertEquals("localhost", server.envs().get(0).hosts().get(0).name);
        assertEquals("127.0.0.1", server.envs().get(0).hosts().get(1).name);
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsSet {
        Set<Environment> environments();

        interface Environment {
            String name();

            Set<App> apps();

            interface App {
                String name();

                Set<String> services();

                Optional<Set<String>> databases();
            }
        }
    }

    @Test
    void mappingCollectionsSet() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsSet.class, "server")
                .withSources(config(
                        "server.environments[0].name", "dev",
                        "server.environments[0].apps[0].name", "rest",
                        "server.environments[0].apps[0].services", "a,b,c",
                        "server.environments[0].apps[0].databases", "pg,h2",
                        "server.environments[0].apps[1].name", "batch",
                        "server.environments[0].apps[1].services", "a,b,c",
                        "server.environments[1].name", "prod",
                        "server.environments[1].apps[0].name", "web",
                        "server.environments[1].apps[0].services", "a,b,c",
                        "server.environments[1].apps[1].name", "rest",
                        "server.environments[1].apps[1].services", "a,b,c",
                        "server.environments[1].apps[2].name", "batch",
                        "server.environments[1].apps[2].services", "a,b,c"))
                .build();
        ServerCollectionsSet server = config.getConfigMapping(ServerCollectionsSet.class);

        assertEquals(2, server.environments().size());

        Optional<Environment> dev = server.environments().stream().filter(environment -> environment.name().equals("dev"))
                .findFirst();
        assertTrue(dev.isPresent());
        dev.ifPresent(environment -> {
            assertEquals(2, environment.apps().size());
            Optional<App> rest = environment.apps().stream().filter(app -> app.name().equals("rest")).findFirst();
            assertTrue(rest.isPresent());
            rest.ifPresent(app -> {
                assertTrue(Stream.of("a", "b", "c").collect(toSet()).containsAll(app.services()));
                assertTrue(app.databases().isPresent());
                assertTrue(Stream.of("pg", "h2").collect(toSet()).containsAll(app.databases().get()));
            });
        });
    }

    @ConfigMapping(prefix = "server")
    public interface ServerOptionalGroup {
        Optional<Environment> environment();

        interface Environment {
            List<String> services();

            List<App> apps();

            interface App {
                String name();
            }
        }
    }

    @Test
    void mappingOptionalGroupWithCollection() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerOptionalGroup.class, "server")
                .withSources(config(
                        "server.environment.services[0]", "rest",
                        "server.environment.services[1]", "batch",
                        "server.environment.apps[0].name", "a"))
                .build();

        ServerOptionalGroup server = config.getConfigMapping(ServerOptionalGroup.class);
        assertTrue(server.environment().isPresent());
        assertEquals("rest", server.environment().get().services().get(0));
        assertEquals("batch", server.environment().get().services().get(1));
        assertEquals("a", server.environment().get().apps().get(0).name());

        config = new SmallRyeConfigBuilder()
                .withMapping(ServerOptionalGroup.class, "server")
                .withSources(config(
                        "server.environment.services", "rest,batch",
                        "server.environment.apps[0].name", "a"))
                .build();

        server = config.getConfigMapping(ServerOptionalGroup.class);
        assertTrue(server.environment().isPresent());
        assertEquals("rest", server.environment().get().services().get(0));
        assertEquals("batch", server.environment().get().services().get(1));
        assertEquals("a", server.environment().get().apps().get(0).name());
    }
}
