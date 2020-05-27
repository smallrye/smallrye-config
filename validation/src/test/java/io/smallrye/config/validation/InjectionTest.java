package io.smallrye.config.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class InjectionTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("memory", null, 0, "/",
                        new InMemoryStreamHandler(
                                "io.smallrye.config.validation.InjectionTestConfigFactory"))
        }, contextClassLoader);
        Thread.currentThread().setContextClassLoader(urlClassLoader);
    }

    @AfterClass
    public static void afterClass() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(contextClassLoader.getParent());
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
