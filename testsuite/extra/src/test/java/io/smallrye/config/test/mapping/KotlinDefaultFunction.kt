package io.smallrye.config.test.mapping

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "server")
interface KotlinDefaultFunction {
    fun host(): String

    fun serverUrl(): String = "https://${host()}"
}
