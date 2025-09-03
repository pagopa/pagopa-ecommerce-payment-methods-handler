package it.pagopa.ecommerce.services;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenService {

    public String getToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    }
}
