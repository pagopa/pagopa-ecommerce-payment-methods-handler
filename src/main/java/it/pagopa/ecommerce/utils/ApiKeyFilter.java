package it.pagopa.ecommerce.utils;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ApiKeyFilter {

    private static final Logger LOG = Logger.getLogger(ApiKeyFilter.class);

    @ConfigProperty(name = "security.apiKey.primary")
    String primaryApiKey;

    @ConfigProperty(name = "security.apiKey.secondary")
    String secondaryApiKey;

    @ConfigProperty(name = "security.apiKey.securedPaths")
    List<String> securedPaths;

    private Set<String> getValidApiKeys() {
        return Set.of(primaryApiKey, secondaryApiKey);
    }

    @RouteFilter
    void filter(RoutingContext ctx) {
        String path = ctx.request().path();

        if (securedPaths.stream().anyMatch(path::startsWith)) {
            String apiKey = ctx.request().getHeader("x-api-key");

            if (!isValidApiKey(apiKey)) {
                LOG.errorf("Unauthorized request for path %s - Missing or invalid API key", path);
                ctx.response().setStatusCode(401).end();
                return;
            }

            logWhichApiKey(apiKey, path);
        }

        ctx.next();
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty() && getValidApiKeys().contains(apiKey);
    }

    private void logWhichApiKey(String apiKey, String path) {
        String apiKeyType;
        if (primaryApiKey.equals(apiKey)) {
            apiKeyType = "primary";
        } else if (secondaryApiKey.equals(apiKey)) {
            apiKeyType = "secondary";
        } else {
            apiKeyType = "unknown";
        }
        LOG.debugf("API key type used for path %s: %s", path, apiKeyType);
    }
}