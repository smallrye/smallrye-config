package io.smallrye.config.examples.mapping;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ServerMappingTest {
    @Test
    void mapping() {
        final Server server = ServerMapping.getServer();
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
        assertEquals(Duration.ofSeconds(60), server.timeout());
        assertEquals(200, server.threads());

        assertEquals("login.html", server.form().get("login-page"));
        assertEquals("error.html", server.form().get("error-page"));
        assertEquals("index.html", server.form().get("landing-page"));

        assertTrue(server.ssl().isPresent());
        assertEquals(8443, server.ssl().get().port());
        assertEquals(Stream.of("TLSv1.3", "TLSv1.2").collect(toList()), server.ssl().get().protocols());

        assertFalse(server.proxy().isPresent());

        assertFalse(server.log().enabled());
        assertEquals(".log", server.log().suffix());
        assertTrue(server.log().rotate());
        assertEquals(Server.Log.Pattern.COMMON, server.log().pattern());

        assertTrue(server.cors().isPresent());
        assertEquals("some-server", server.cors().get().origins().get(0).host());
        assertEquals(9000, server.cors().get().origins().get(0).port());
        assertEquals("another-server", server.cors().get().origins().get(1).host());
        assertEquals(8000, server.cors().get().origins().get(1).port());
        assertEquals("GET", server.cors().get().methods().get(0));
        assertEquals("POST", server.cors().get().methods().get(1));
    }
}
