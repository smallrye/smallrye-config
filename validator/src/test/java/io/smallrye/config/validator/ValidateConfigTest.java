package io.smallrye.config.validator;

import static io.smallrye.config.validator.KeyValuesConfigSource.config;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithParentName;

public class ValidateConfigTest {

    private static Locale oldDefaultLocale = Locale.getDefault();

    /**
     * Set the default locale to ROOT before the tests as the validation problem messages are locale-sensitive.
     */
    @BeforeAll
    static void setupMessageLocale() {
        Locale.setDefault(Locale.ROOT);
    }

    /**
     * Restore the old default locale just in case it is needed elsewhere outside this test class.
     */
    @AfterAll
    static void restoreMessageLocale() {
        Locale.setDefault(oldDefaultLocale);
    }

    @Test
    void validateConfigMapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withSources(config(
                        "server.host", "localhost",
                        "server.port", "8080",
                        "server.log.days", "20",
                        "server.log.levels.INFO.importance", "1",
                        "server.log.levels.INFO.description", "just info",
                        "server.log.levels.ERROR.importance", "-1",
                        "server.log.levels.ERROR.description", "serious error",
                        "server.proxy.enable", "true",
                        "server.proxy.timeout", "20",
                        "server.form.login-page", "login.html",
                        "server.form.error-page", "error.html",
                        "server.form.landing-page", "index.html",
                        "server.form.x", "xyz",
                        "server.cors.origins[0].host", "some-server",
                        "server.cors.origins[0].port", "9000",
                        "server.cors.origins[1].host", "localhost",
                        "server.cors.origins[1].port", "1",
                        "server.cors.methods[0]", "GET",
                        "server.cors.methods[1]", "POST",
                        "server.info.name", "Bond",
                        "server.info.code", "007",
                        "server.info.alias[0]", "James",
                        "server.info.admins.root[0].username", "root",
                        "server.info.admins.root[1].username", "admin",
                        "server.info.firewall.accepted[0]", "127.0.0.1",
                        "server.info.firewall.accepted[1]", "8.8.8"))
                .withMapping(Server.class, "server")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(Server.class, "server"));
        List<String> validations = new ArrayList<>();
        for (int i = 0; i < validationException.getProblemCount(); i++) {
            validations.add(validationException.getProblem(i).getMessage());
        }
        assertValidationsEqual(validations,
                "server.port must be less than or equal to 10",
                "server.log.days must be less than or equal to 15",
                "server.log.levels all identifiers must have the same length",
                "server.log.levels.ERROR.importance must be greater than or equal to 0",
                "server.proxy.timeout must be less than or equal to 10",
                "server.cors.origins size must be between 3 and 2147483647",
                "server.cors.origins[0].host size must be between 0 and 10",
                "server.cors.origins[0].port must be less than or equal to 10",
                "server.cors.methods[1] size must be between 0 and 3",
                "server.cors.methods size must be between 3 and 2147483647",
                "server.form.login-page size must be between 0 and 3",
                "server.form.error-page size must be between 0 and 3",
                "server.form.landing-page size must be between 0 and 3",
                "server.form.x size must be between 2 and 2147483647",
                "server.info.name size must be between 0 and 3",
                "server.info.code must be less than or equal to 3",
                "server.info.alias[0] size must be between 0 and 3",
                "server.info.admins.root[1].username size must be between 0 and 4",
                "server.info.admins.root size must be between 0 and 1",
                "server.info.firewall.accepted[1] size must be between 8 and 15",
                "server is not prod");
    }

    @Test
    void validateNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withSources(config(
                        "server.the_host", "localhost",
                        "server.the_port", "8080"))
                .withMapping(ServerNamingStrategy.class, "server")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(ServerNamingStrategy.class, "server"));
        List<String> validations = new ArrayList<>();
        for (int i = 0; i < validationException.getProblemCount(); i++) {
            validations.add(validationException.getProblem(i).getMessage());
        }

        assertTrue(validations.contains("server.the_port must be less than or equal to 10"));
    }

    @Test
    void validateConfigProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withMapping(Client.class, "client")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(Client.class, "client"));
        assertEquals(1, validationException.getProblemCount());
        List<String> validations = new ArrayList<>();
        validations.add(validationException.getProblem(0).getMessage());
        assertTrue(validations.contains("port must be less than or equal to 10"));
    }

    @Test
    void validateParent() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withSources(config(
                        "server.host", "localhost",
                        "server.port", "80"))
                .withMapping(ServerParent.class, "server")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(ServerParent.class, "server"));
        assertEquals("server.port must be greater than or equal to 8000", validationException.getProblem(0).getMessage());
    }

    @ConfigMapping(prefix = "server")
    @Prod
    public interface Server {
        String host();

        @Max(10)
        int port();

        Map<@Size(min = 2) String, @Size(max = 3) String> form();

        Optional<Proxy> proxy();

        Log log();

        Cors cors();

        Info info();

        interface Proxy {
            boolean enable();

            @Max(10)
            int timeout();
        }

        interface Log {
            @Max(15)
            int days();

            @EqualLengthKeys
            Map<String, LogLevel> levels();

            interface LogLevel {

                @PositiveOrZero
                int importance();

                @NotBlank
                String description();
            }
        }

        interface Cors {
            @Size(min = 3)
            List<Origin> origins();

            @Size(min = 3)
            List<@Size(max = 3) String> methods();

            interface Origin {
                @Size(max = 10)
                String host();

                @Max(10)
                int port();
            }
        }

        interface Info {
            Optional<@Size(max = 3) String> name();

            @Max(3)
            OptionalInt code();

            Optional<List<@Size(max = 3) String>> alias();

            Map<String, @Size(max = 1) List<Admin>> admins();

            Map<String, List<@Size(min = 8, max = 15) String>> firewall();

            interface Admin {
                @Size(max = 4)
                String username();
            }
        }
    }

    @Target({ TYPE, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = { ServerValidator.class })
    public @interface Prod {
        String message() default "server is not prod";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class ServerValidator implements ConstraintValidator<Prod, Server> {
        @Override
        public boolean isValid(final Server value, final ConstraintValidatorContext context) {
            return value.host().equals("prod");
        }
    }

    @Target({ METHOD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = { EqualLengthKeysValidator.class })
    public @interface EqualLengthKeys {
        String message() default "all identifiers must have the same length";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class EqualLengthKeysValidator implements ConstraintValidator<EqualLengthKeys, Map<String, ?>> {
        @Override
        public boolean isValid(final Map<String, ?> value, final ConstraintValidatorContext context) {
            return value.keySet().stream().mapToInt(String::length).distinct().count() <= 1;
        }
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    public interface ServerNamingStrategy {
        String theHost();

        @Max(10)
        int thePort();
    }

    @ConfigProperties(prefix = "client")
    public static class Client {
        public String host = "localhost";
        @Max(10)
        public int port = 8080;
    }

    @ConfigMapping(prefix = "server")
    public interface ServerParent {
        @WithParentName
        Parent parent();

        interface Parent {
            String host();

            @Min(8000)
            int port();
        }
    }

    @Test
    void optionalLists() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withMapping(Optionals.class)
                .withSources(config(
                        "optionals.list[0].value", "value",
                        "optionals.list-map[0].value", "value"))
                .build();

        Optionals mapping = config.getConfigMapping(Optionals.class);

        assertTrue(mapping.list().isPresent());
        assertEquals("value", mapping.list().get().get(0).value());
        assertTrue(mapping.listMap().isPresent());
        assertEquals("value", mapping.listMap().get().get(0).get("value"));
    }

    @ConfigMapping(prefix = "optionals")
    interface Optionals {
        Optional<List<Nested>> list();

        Optional<List<Map<String, String>>> listMap();

        interface Nested {
            String value();
        }
    }

    @Test
    void hierarchy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withMapping(Child.class)
                .withSources(config("validator.child.number", "1"))
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(Child.class));
        List<String> validations = new ArrayList<>();
        for (int i = 0; i < validationException.getProblemCount(); i++) {
            validations.add(validationException.getProblem(i).getMessage());
        }
        assertEquals(1, validations.size());
        assertTrue(validations.contains("validator.child.number must be greater than or equal to 10"));
    }

    public interface Parent {
        @Min(10)
        Integer number();
    }

    @ConfigMapping(prefix = "validator.child")
    public interface Child extends Parent {

    }

    private static void assertValidationsEqual(List<String> validations, String... expectedProblemMessages) {
        List<String> remainingActual = new ArrayList<>(validations);
        List<String> remainingExpected = Stream.of(expectedProblemMessages).collect(Collectors.toList());
        computeListDifference(remainingActual, remainingExpected);
        StringBuilder failureMessage = new StringBuilder();
        addFailureToMessage("The following validation problems are missing:", remainingExpected, failureMessage);
        addFailureToMessage("The following validation problems were not expected:", remainingActual, failureMessage);
        if (failureMessage.length() > 0) {
            fail(failureMessage.toString());
        }
    }

    private static void computeListDifference(List<String> remainingActual, List<String> remainingExpected) {
        // Not using removeAll here as that would erase duplicates, whereas this "Removes the first occurrence [..]"
        Iterator<String> remainingExpectedIterator = remainingExpected.iterator();
        while (remainingExpectedIterator.hasNext()) {
            boolean expectedFound = remainingActual.remove(remainingExpectedIterator.next());
            if (expectedFound) {
                remainingExpectedIterator.remove();
            }
        }
    }

    private static void addFailureToMessage(String description, List<String> list, StringBuilder failureMessage) {
        if (!list.isEmpty()) {
            failureMessage.append("\n");
            failureMessage.append(description);
            for (String element : list) {
                failureMessage.append("\n  \"");
                failureMessage.append(element);
                failureMessage.append("\"");
            }
        }
    }
}
