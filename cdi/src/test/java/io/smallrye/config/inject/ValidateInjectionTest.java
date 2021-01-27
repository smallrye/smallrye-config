package io.smallrye.config.inject;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.assertj.core.api.Condition;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.inject.InjectionTestConfigFactory.ConvertedValue;

public class ValidateInjectionTest {
    @Test
    void missingProperty() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(MissingPropertyTest.class)).build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed().assertEventsMatchExactly(finishedWithFailure(instanceOf(DeploymentException.class),
                message("SRCFG02000: No Config Value exists for required property missing.property")));
    }

    @Test
    void converterMissingProperty() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(ConverterMissingPropertyTest.class)).build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed().assertEventsMatchExactly(finishedWithFailure(instanceOf(DeploymentException.class),
                message("SRCFG02000: No Config Value exists for required property missing.property")));
    }

    @Test
    void skipProperties() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(SkipPropertiesTest.class)).build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed().assertEventsMatchExactly(finishedWithFailure(instanceOf(DeploymentException.class),
                message("SRCFG02000: No Config Value exists for required property missing.property")));
    }

    @Test
    void constructorUnnamedProperties() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(ConstructorUnnamedPropertiesTest.class)).build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed()
                .assertEventsMatchExactly(finishedWithFailure(instanceOf(DeploymentException.class), new Condition<>(
                        throwable -> throwable.getMessage()
                                .startsWith("SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint"),
                        "")));
    }

    @Test
    void methodUnnamedProperties() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(MethodUnnamedPropertiesTest.class)).build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed()
                .assertEventsMatchExactly(
                        finishedWithFailure(instanceOf(DeploymentException.class), new Condition<>(
                                throwable ->
                                // Ensure both invalid methods are included in the error message
                                throwable.getMessage()
                                        .contains("SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint")
                                        && throwable.getMessage().contains("setUnnamedA")
                                        && throwable.getMessage().contains("setUnnamedB"),
                                "")));
    }

    @Test
    void unqualifiedConfigPropertiesInjection() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(UnqualifiedConfigPropertiesInjectionTest.class))
                .build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed()
                .assertEventsMatchExactly(finishedWithFailure(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void missingPropertyExpressionInjection() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(MissingPropertyExpressionInjectionTest.class))
                .build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed().assertEventsMatchExactly(finishedWithFailure(instanceOf(DeploymentException.class),
                message("SRCFG00011: Could not expand value missing.prop in property bad.property.expression.prop")));
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MissingPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MissingPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MissingPropertyBean missingPropertyBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class MissingPropertyBean {
            @Inject
            @ConfigProperty(name = "missing.property")
            String missing;
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class ConverterMissingPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ConverterMissingPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        ConverterMissingPropertyBean converterMissingPropertyBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class ConverterMissingPropertyBean {
            @Inject
            @ConfigProperty(name = "missing.property")
            ConvertedValue missing;
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class SkipPropertiesTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(SortInjectionPointsExtension.class, SkipPropertiesBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        SkipPropertiesBean skipPropertiesBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class SkipPropertiesBean {
            @Inject
            @ConfigProperty(name = "skip.property")
            ConfigValue skip;
            @Inject
            @ConfigProperty(name = "missing.property")
            String missing;
        }

        public static class SortInjectionPointsExtension extends ConfigExtension {
            // Make sure we test the skiped property first for the validation to continue.
            @Override
            protected Set<InjectionPoint> getConfigPropertyInjectionPoints() {
                return super.getConfigPropertyInjectionPoints().stream().sorted((o1, o2) -> {
                    if (o1.getMember().getName().equals("skip")) {
                        return -1;
                    }
                    return 0;
                }).collect(Collectors.toCollection(LinkedHashSet::new));
            }
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class ConstructorUnnamedPropertiesTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ConstructorUnnamedPropertiesBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        ConstructorUnnamedPropertiesBean constructorUnnamedPropertiesBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class ConstructorUnnamedPropertiesBean {
            private final String unnamed;

            @Inject
            public ConstructorUnnamedPropertiesBean(@ConfigProperty final String unnamed) {
                this.unnamed = unnamed;
            }
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MethodUnnamedPropertiesTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MethodUnnamedPropertiesBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MethodUnnamedPropertiesBean bean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class MethodUnnamedPropertiesBean {

            @Inject
            private void setUnnamedA(@ConfigProperty String unnamedA) {
            }

            @Inject
            private void setUnnamedB(@ConfigProperty String unnamedB) {
            }
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class UnqualifiedConfigPropertiesInjectionTest extends InjectionTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, Server.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        Server server;

        @Test
        void fail() {
            Assertions.fail();
        }

        @Dependent
        @ConfigProperties(prefix = "server")
        public static class Server {
            public String host;
            public int port;
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MissingPropertyExpressionInjectionTest extends InjectionTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MissingPropertyExpressionBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MissingPropertyExpressionBean bean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class MissingPropertyExpressionBean {
            @Inject
            @ConfigProperty(name = "bad.property.expression.prop")
            String missing;
        }
    }
}
