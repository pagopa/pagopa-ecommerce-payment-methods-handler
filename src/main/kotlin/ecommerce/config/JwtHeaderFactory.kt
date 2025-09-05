package ecommerce.config

import ecommerce.services.TokenService
import jakarta.inject.Inject
import jakarta.ws.rs.core.MultivaluedMap
import java.util.UUID
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class JwtHeaderFactory : ClientHeadersFactory {

    @Inject lateinit var tokenService: TokenService

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>?,
        clientOutgoingHeaders: MultivaluedMap<String, String>?,
    ): MultivaluedMap<String, String>? {
        val token = tokenService.getToken()
        clientOutgoingHeaders?.add("Authorization", "Bearer $token")
        clientOutgoingHeaders?.add("X-request-uuid", UUID.randomUUID().toString())
        return clientOutgoingHeaders
    }
}
