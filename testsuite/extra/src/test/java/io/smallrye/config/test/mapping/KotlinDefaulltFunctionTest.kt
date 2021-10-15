package io.smallrye.config.test.mapping

import io.smallrye.config.SmallRyeConfigBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinDefaulltFunctionTest {
    @Test
    fun kotlinDefaultMethod() {
        val config = SmallRyeConfigBuilder()
                .withMapping(KotlinDefaultFunction::class.java, "server")
                .withDefaultValue("server.host", "localhost")
                .build()

        val mapping = config.getConfigMapping(KotlinDefaultFunction::class.java)
        assertEquals("localhost", mapping.host())
        assertEquals("https://localhost", mapping.serverUrl())
    }
}
