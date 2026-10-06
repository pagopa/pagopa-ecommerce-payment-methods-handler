package it.pagopa.ecommerce.payment.methods.config

import io.smallrye.config.ConfigMapping
import java.net.URI

@ConfigMapping(prefix = "session-url")
interface SessionUrlConfig {
    fun basePath(): URI

    fun ioBasePath(): URI

    fun outcomeSuffix(): String

    fun cancelSuffix(): String

    fun notificationUrl(): String
}
