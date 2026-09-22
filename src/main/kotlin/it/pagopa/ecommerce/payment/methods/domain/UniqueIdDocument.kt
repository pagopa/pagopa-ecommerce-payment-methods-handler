package it.pagopa.ecommerce.payment.methods.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * Redis document holding volatile information about a generated unique id.
 *
 * This mirrors the structure of `UniqueIdDocument` in pagopa-ecommerce-commons (`id` +
 * `creationDate`) so that the value stored under the shared `uniqueId` keyspace is compatible
 * across services.
 */
data class UniqueIdDocument
@JsonCreator
constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("creationDate") val creationDate: String,
) {
    constructor(id: String) : this(id, OffsetDateTime.now().toString())
}
