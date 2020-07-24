package io.smallrye.config;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class SmallRyeConfigArchiveProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            WebArchive war = (WebArchive) applicationArchive;
            war.addAsServiceProvider(SmallRyeConfigFactory.class, useFactoryForTest(testClass));
        }
    }

    private static Class<?> useFactoryForTest(TestClass testClass) {
        return SmallRyeConfigTestFactory.class;
    }
}
