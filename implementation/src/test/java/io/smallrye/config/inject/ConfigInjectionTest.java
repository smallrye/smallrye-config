package io.smallrye.config.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ConfigInjectionTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("memory", null, 0, "/",
                        new InMemoryStreamHandler("io.smallrye.config.inject.ConfigInjectionTestConfigFactory"))
        }, contextClassLoader);
        Thread.currentThread().setContextClassLoader(urlClassLoader);
    }

    @After
    public void afterClass() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(contextClassLoader.getParent());
    }

    @Rule
    public WeldInitiator weld = WeldInitiator.from(ConfigProducer.class, ConfigBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    private ConfigBean configBean;

    @Test
    public void inject() {
        assertEquals("1234", configBean.getMyProp());
        assertEquals("1234", configBean.getExpansion());
        assertEquals("12345678", configBean.getSecret());

        assertThrows("Not allowed to access secret key secret", SecurityException.class,
                () -> configBean.getConfig().getValue("secret", String.class));
    }

    @ApplicationScoped
    public static class ConfigBean {
        @Inject
        @ConfigProperty(name = "my.prop")
        private String myProp;
        @Inject
        @ConfigProperty(name = "expansion")
        private String expansion;
        @Inject
        @ConfigProperty(name = "secret")
        private String secret;
        @Inject
        private Config config;

        String getMyProp() {
            return myProp;
        }

        String getExpansion() {
            return expansion;
        }

        String getSecret() {
            return secret;
        }

        Config getConfig() {
            return config;
        }
    }

    public static class InMemoryStreamHandler extends URLStreamHandler {
        final byte[] contents;

        public InMemoryStreamHandler(final String contents) {
            this.contents = contents.getBytes();
        }

        @Override
        protected URLConnection openConnection(final URL u) throws IOException {
            if (!u.getFile().endsWith("SmallRyeConfigFactory")) {
                return null;
            }

            return new URLConnection(u) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(contents);
                }
            };
        }
    }
}
