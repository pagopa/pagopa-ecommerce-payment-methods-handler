package it.pagopa.ecommerce.config;

import it.pagopa.ecommerce.services.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.UUID;

public class JwtHeaderFactory implements ClientHeadersFactory {

    @Inject
    TokenService tokenService;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders
    ) {
        //String token = tokenService.getToken();
        //clientOutgoingHeaders.add("Authorization", "Bearer " + token);
        clientOutgoingHeaders.add("X-request-uuid", UUID.randomUUID().toString());
        return clientOutgoingHeaders;
    }
}