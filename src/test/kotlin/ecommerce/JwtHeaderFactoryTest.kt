package ecommerce

import ecommerce.config.JwtHeaderFactory
import ecommerce.services.TokenService
import jakarta.ws.rs.core.MultivaluedHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class JwtHeaderFactoryTest {

    private val tokenService = mock<TokenService>()
    private val factory = JwtHeaderFactory()

    @BeforeEach
    fun setup() {
        factory.tokenService = tokenService
    }

    @Test
    fun `should add Authorization header when tokenService is available`() {
        whenever(tokenService.getToken()).thenReturn("mocked-token")

        val outgoing = MultivaluedHashMap<String, String>()
        val result = factory.update(null, outgoing)

        assertEquals("Bearer mocked-token", result!!.getFirst("Authorization"))

        val uuidHeader = result!!.getFirst("X-request-uuid")
        assertNotNull(uuidHeader, "X-request-uuid should not be null")
        assertTrue(uuidHeader.isNotBlank(), "X-request-uuid should not be blank")
    }
}
