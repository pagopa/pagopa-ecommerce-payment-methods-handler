package it.pagopa.ecommerce.payment.methods.client

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.quarkus.test.junit.QuarkusTest
import it.pagopa.generated.ecommerce.npg.client.api.NpgBuildIntegrityApi
import jakarta.inject.Inject
import java.util.*
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@QuarkusTest
class NpgBuildIntegrityApiIntegrationTest {

    @Inject @RestClient lateinit var npgBuildIntegrityApi: NpgBuildIntegrityApi

    companion object {
        @JvmStatic
        @RegisterExtension
        val wireMock =
            WireMockExtension.newInstance()
                .options(WireMockConfiguration.wireMockConfig().port(8089))
                .build()
    }

    @Test
    fun `should receive mocked build integrity response`() {
        val expectedHash = "sha384-dhR+dKVjiXoA8ZNODO+zKzgioWlnNMSKaLbNPT/GdTn0NgIoA4ke+4/bq6CfQjpJ"
        val mockResponse =
            """
            {
              "integrity": "$expectedHash"
            }
        """
                .trimIndent()

        wireMock.stubFor(
            get(urlEqualTo("/api/phoenix-0.0/psp/api/v1/build/integrity"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)
                )
        )

        val response =
            npgBuildIntegrityApi.getBuildIntegrity(UUID.randomUUID()).await().indefinitely()

        assertNotNull(response)
        assertEquals(expectedHash, response.integrity)
    }

    @Test
    fun `should handle error response from NPG`() {
        wireMock.stubFor(
            get(urlEqualTo("/api/phoenix-0.0/psp/api/v1/build/integrity"))
                .willReturn(aResponse().withStatus(500))
        )

        assertThrows(Exception::class.java) {
            npgBuildIntegrityApi.getBuildIntegrity(UUID.randomUUID()).await().indefinitely()
        }
    }
}
