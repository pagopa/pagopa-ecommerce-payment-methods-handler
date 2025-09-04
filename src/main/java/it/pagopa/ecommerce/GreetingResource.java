package it.pagopa.ecommerce;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/hello")
public class GreetingResource {
    private static final Logger LOG = Logger.getLogger(GreetingResource.class);


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("Hello RESTEasy");
        return "Hello RESTEasy";
    }
}
