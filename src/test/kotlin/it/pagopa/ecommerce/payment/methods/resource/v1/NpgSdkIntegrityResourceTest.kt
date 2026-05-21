package it.pagopa.ecommerce.payment.methods.resource.v1

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.client.NpgClient
import it.pagopa.ecommerce.payment.methods.exception.NpgClientException
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgSdkIntegrityResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import it.pagopa.generated.ecommerce.npg.client.model.BuildIntegrityResponseDto
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@QuarkusTest
class NpgSdkIntegrityResourceTest {

    @InjectMock lateinit var mockNpgClient: NpgClient

    @Test
    fun `should return OK response with integrity hash`() {
        val expectedHash = "sha384-dhR+dKVjiXoA8ZNODO+zKzgioWlnNMSKaLbNPT/GdTn0NgIoA4ke+4/bq6CfQjpJ"
        val mockResponse = BuildIntegrityResponseDto().apply { integrity = expectedHash }

        whenever(mockNpgClient.getIntegrity(anyOrNull())).then {
            Uni.createFrom().item(mockResponse)
        }

        val result =
            RestAssured.given()
                .header("x-api-key", "test-primary")
                .`when`()
                .get("/npg/sdk/integrity")
                .then()
                .statusCode(200)
                .extract()
                .`as`(NpgSdkIntegrityResponse::class.java)

        assertNotNull(result)
        assertEquals(expectedHash, result.integrityHash)
    }

    @Test
    fun `should return 500 when NPG client fails`() {
        whenever(mockNpgClient.getIntegrity(anyOrNull())).then {
            Uni.createFrom()
                .failure<BuildIntegrityResponseDto>(
                    NpgClientException("NPG call failed", RuntimeException("Connection refused"))
                )
        }

        val result =
            RestAssured.given()
                .header("x-api-key", "test-primary")
                .`when`()
                .get("/npg/sdk/integrity")
                .then()
                .statusCode(500)
                .extract()
                .`as`(ProblemJson::class.java)

        assertNotNull(result)
        assertEquals(500, result.status)
    }

    @Test
    fun `should return 401 without api key`() {
        RestAssured.given().`when`().get("/npg/sdk/integrity").then().statusCode(401)
    }
}
