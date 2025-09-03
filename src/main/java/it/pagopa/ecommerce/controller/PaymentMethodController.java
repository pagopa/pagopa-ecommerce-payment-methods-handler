package it.pagopa.ecommerce.controller;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import it.pagopa.ecommerce.dto.PaymentMethod;
import it.pagopa.ecommerce.services.PaymentMethodService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

@Path("/payment-method")
public class PaymentMethodController {

    @Inject
    @RestClient
    PaymentMethodService paymentMethodService;

    private final Logger LOGGER = Logger.getLogger(PaymentMethodController.class);


    @GET
    @Path("/all")
    public Set<PaymentMethod> getAllPaymentMethods(@PathParam String id) {
        LOGGER.info("CALL GET ALL PAYMENT-METHOD");
        return paymentMethodService.getAll();
    }

    @GET
    @Path("/all-async")
    public CompletionStage<PaymentMethod> getAllPaymentMethodsAsync(@PathParam String id) {
        return paymentMethodService.getAllAsync();
    }

    @GET
    @Path("/all-as-uni")
    public Uni<PaymentMethod> getAllPaymentMethodsAsUni(@PathParam String id) {
        return paymentMethodService.getAllAsUni();
    }

    @GET
    @Path("/id/{id}")
    public Set<PaymentMethod> getPaymentMethod(@PathParam String id) {
        return paymentMethodService.getById(id);
    }

    @GET
    @Path("/id-async/{id}")
    public CompletionStage<Set<PaymentMethod>> getPaymentMethodAsync(@PathParam String id) {
        return paymentMethodService.getByIdAsync(id);
    }

    @GET
    @Path("/id-uni/{id}")
    public Uni<Set<PaymentMethod>> getPaymentMethodMutiny(@PathParam String id) {
        return paymentMethodService.getByIdAsUni(id);
    }
}
