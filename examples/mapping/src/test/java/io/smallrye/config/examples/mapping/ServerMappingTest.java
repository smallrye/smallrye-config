package io.smallrye.config.examples.mapping;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class ServerMappingTest {
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
    }
}
