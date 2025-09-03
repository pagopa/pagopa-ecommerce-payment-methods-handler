package it.pagopa.ecommerce.services;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import it.pagopa.ecommerce.config.JwtHeaderFactory;
import it.pagopa.ecommerce.dto.PaymentMethod;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import io.smallrye.mutiny.Uni;

@Path("/payment-method")
@RegisterRestClient
@RegisterClientHeaders(JwtHeaderFactory.class)
public interface PaymentMethodService {

    @GET
    Set<PaymentMethod> getAll();

    @GET
    CompletionStage<PaymentMethod> getAllAsync();

    @GET
    Uni<PaymentMethod> getAllAsUni();

    @GET
    Set<PaymentMethod> getById(@QueryParam String id);

    @GET
    CompletionStage<Set<PaymentMethod>> getByIdAsync(@QueryParam String id);

    @GET
    Uni<Set<PaymentMethod>> getByIdAsUni(@QueryParam String id);
}
