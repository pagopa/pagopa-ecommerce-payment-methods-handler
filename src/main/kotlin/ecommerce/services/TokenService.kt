package ecommerce.services

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class TokenService {

    fun getToken(): String {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
}
