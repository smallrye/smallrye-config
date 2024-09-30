package io.smallrye.config.test.mapping

import io.smallrye.config.ConfigMapping
import io.smallrye.config.SmallRyeConfigBuilder
import io.smallrye.config.WithDefault
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinMappingGetter {
    @Test
    fun mappingGetterNames() {
        val config = SmallRyeConfigBuilder()
            .withMapping(GraphOption::class.java, "graph")
            .withDefaultValue("graph.get-tenant", "tenant")
            .withDefaultValue("graph.get-client-id", "id")
            .withDefaultValue("graph.get-client-secret", "secret")
            .build()

        val mapping = config.getConfigMapping(GraphOption::class.java)
        assertEquals("url", mapping.baseUrl)
        assertEquals("tenant", mapping.tenant)
        assertEquals("id", mapping.clientId)
        assertEquals("secret", mapping.clientSecret)
    }

    @ConfigMapping(prefix = "graph")
    interface GraphOption {
        @get:WithDefault("url")
        val baseUrl: String
        val tenant: String
        val clientId: String
        val clientSecret: String
    }
}
