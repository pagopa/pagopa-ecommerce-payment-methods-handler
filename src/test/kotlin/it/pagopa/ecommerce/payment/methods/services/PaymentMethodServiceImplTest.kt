package it.pagopa.ecommerce.payment.methods.services

import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.TestUtils
import it.pagopa.ecommerce.payment.methods.client.CreateTokenResponse
import it.pagopa.ecommerce.payment.methods.client.NpgFieldDto
import it.pagopa.ecommerce.payment.methods.client.NpgFieldsDto
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.domain.NpgSessionDocument
import it.pagopa.ecommerce.payment.methods.exception.JwtIssuerResponseException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.ws.rs.core.Response
import java.net.URI
import java.time.LocalDate
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@QuarkusTest
class PaymentMethodsClientTest {

    private val mockClient = Mockito.mock(PaymentMethodsClient::class.java)
    private val mockNpgClient =
        Mockito.mock(it.pagopa.ecommerce.payment.methods.client.NpgClientWrapper::class.java)
    private val mockJwtClient =
        Mockito.mock(it.pagopa.ecommerce.payment.methods.client.JwtTokenIssuerClient::class.java)
    private val mockNpgSessionsRedis =
        Mockito.mock(
            it.pagopa.ecommerce.payment.methods.infrastructure.NpgSessionsRedisWrapper::class.java
        )
    private val mockUniqueIdGenerator =
        Mockito.mock(it.pagopa.ecommerce.payment.methods.utils.UniqueIdGenerator::class.java)
    private val mockSessionUrlConfig =
        Mockito.mock(it.pagopa.ecommerce.payment.methods.config.SessionUrlConfig::class.java)
    private val service =
        PaymentMethodServiceImpl(
            mockClient,
            mockNpgClient,
            mockJwtClient,
            mockNpgSessionsRedis,
            mockUniqueIdGenerator,
            mockSessionUrlConfig,
            900,
        )

    private val mockApi = Mockito.mock(PaymentMethodsApi::class.java)
    private val client = PaymentMethodsClient(mockApi)

    @Test
    fun `should return response from PaymentMethodsApi get all methods`() {
        val requestDto =
            PaymentMethodRequestDto().apply {
                userTouchpoint = PaymentMethodRequestDto.UserTouchpointEnum.CHECKOUT
                userDevice = PaymentMethodRequestDto.UserDeviceEnum.WEB
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
            }

        val expectedResponse =
            PaymentMethodsResponseDto().apply {
                paymentMethods =
                    listOf(
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "card_visa"
                            name = mapOf("it" to "Carta Visa")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = "CP"
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
                        }
                    )
            }

        whenever(mockApi.searchPaymentMethods(requestDto, "test-id"))
            .thenReturn(Uni.createFrom().item(expectedResponse))

        val response = client.searchPaymentMethods(requestDto, "test-id").await().indefinitely()

        assertEquals(1, response.paymentMethods?.size)
        assertEquals("card_visa", response.paymentMethods?.get(0)?.paymentMethodId)
        assertEquals("Carta Visa", response.paymentMethods?.get(0)?.name?.get("it"))
    }

    @Test
    fun `should return payment not found exception when payment method does not exist`() {
        val methodId = "test-id"
        val mockResponse = ClientResponseImpl()
        mockResponse.setStatus(Response.Status.NOT_FOUND.statusCode)

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().failure(ClientWebApplicationException(mockResponse)))

        assertThrows<PaymentMethodNotFoundException> {
            client.getPaymentMethod(methodId, "test-id", "CHECKOUT").await().indefinitely()
        }
    }

    @Test
    fun `should return response from PaymentMethodsApi get method by id`() {
        val methodId = "test-id"
        val expectedResponse =
            PaymentMethodResponseDto().apply {
                paymentMethodId = "test-id"
                name = mapOf("it" to "Carta Visa")
                status = PaymentMethodResponseDto.StatusEnum.ENABLED
                validityDateFrom = LocalDate.of(2025, 1, 1)
                group = "CP"
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
                userTouchpoint = listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                methodManagement = PaymentMethodResponseDto.MethodManagementEnum.ONBOARDABLE
                validityDateFrom = LocalDate.now()
                metadata = mapOf("test" to "test")
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
            }

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().item(expectedResponse))

        val response =
            client.getPaymentMethod(methodId, "test-id", "CHECKOUT").await().indefinitely()

        assertEquals("Carta Visa", response.name?.get("it"))
        assertEquals(methodId, response.paymentMethodId)
    }

    @Test
    fun `should handle failure from PaymentMethodsApi get all methods`() {
        val requestDto = PaymentMethodRequestDto()
        val simulatedError = RuntimeException("API failure")

        whenever(mockApi.searchPaymentMethods(requestDto, "test-id"))
            .thenReturn(Uni.createFrom().failure(simulatedError))

        val thrown =
            assertThrows<PaymentMethodsClientException> {
                client.searchPaymentMethods(requestDto, "test-id").await().indefinitely()
            }

        assertEquals(
            "Error during the call to PaymentMethodsApi.searchPaymentMethods",
            thrown.message,
        )
        assertEquals(simulatedError, thrown.cause)
    }

    @Test
    fun `should handle failure from PaymentMethodsApi get method by id`() {
        val methodId = "test-id"
        val simulatedError = RuntimeException("API failure")

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().failure(simulatedError))

        val thrown =
            assertThrows<PaymentMethodsClientException> {
                client.getPaymentMethod(methodId, "test-id", "CHECKOUT").await().indefinitely()
            }

        assertEquals("Error during the call to PaymentMethodsApi.getPaymentMethod", thrown.message)
        assertEquals(simulatedError, thrown.cause)
    }

    @Test
    fun `should delegate searchPaymentMethods to client`() {
        val expectedResponse = PaymentMethodsResponse()
        val expectedResponseDto = PaymentMethodsResponseDto()

        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .thenReturn(Uni.createFrom().item(expectedResponseDto))

        val result =
            service
                .searchPaymentMethods(TestUtils.buildDefaultMockRequest(), "test-id")
                .toCompletableFuture()
                .get()

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `should return PaymentNotFoundException if retrieved method is not for the provided user touchpoint`() {
        val methodId = "test-id"
        val expectedResponse =
            PaymentMethodResponseDto().apply {
                paymentMethodId = "test-id"
                name = mapOf("it" to "Carta Visa")
                status = PaymentMethodResponseDto.StatusEnum.ENABLED
                validityDateFrom = LocalDate.of(2025, 1, 1)
                group = "CP"
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
                userTouchpoint = listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                methodManagement = PaymentMethodResponseDto.MethodManagementEnum.ONBOARDABLE
                validityDateFrom = LocalDate.now()
                metadata = mapOf("test" to "test")
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
            }

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().item { expectedResponse })

        assertThrows<PaymentMethodNotFoundException> {
            client.getPaymentMethod(methodId, "test-id", "IO").await().indefinitely()
        }
    }

    @Test
    fun `should accept request without any info about language, sortOrder, sortKey and priority groups`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
            }

        assertDoesNotThrow { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should accept request without any info about sortOrder, sortKey and priority groups`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
                language = PaymentMethodsRequest.LanguageEnum.IT
            }

        assertDoesNotThrow { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should accept request without any info about sortOrder and priority groups`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
                language = PaymentMethodsRequest.LanguageEnum.IT
                sortBy = PaymentMethodsRequest.SortByEnum.DESCRIPTION
            }

        assertDoesNotThrow { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should accept request without any info priority groups`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
                language = PaymentMethodsRequest.LanguageEnum.IT
                sortBy = PaymentMethodsRequest.SortByEnum.DESCRIPTION
                sortOrder = PaymentMethodsRequest.SortOrderEnum.ASC
            }

        assertDoesNotThrow { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should accept request with info priority groups as empty list`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
                language = PaymentMethodsRequest.LanguageEnum.IT
                sortBy = PaymentMethodsRequest.SortByEnum.DESCRIPTION
                sortOrder = PaymentMethodsRequest.SortOrderEnum.ASC
                priorityGroups = listOf()
            }

        assertDoesNotThrow { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should accept request with complete info about priority group as one element list`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
                language = PaymentMethodsRequest.LanguageEnum.IT
                sortBy = PaymentMethodsRequest.SortByEnum.DESCRIPTION
                sortOrder = PaymentMethodsRequest.SortOrderEnum.ASC
                priorityGroups = listOf(PaymentMethodsRequest.PriorityGroupsEnum.CP)
            }

        assertDoesNotThrow { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should accept request with complete info about priority group as more element list`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
                language = PaymentMethodsRequest.LanguageEnum.IT
                sortBy = PaymentMethodsRequest.SortByEnum.DESCRIPTION
                sortOrder = PaymentMethodsRequest.SortOrderEnum.ASC
                priorityGroups =
                    listOf(
                        PaymentMethodsRequest.PriorityGroupsEnum.CP,
                        PaymentMethodsRequest.PriorityGroupsEnum.PPAL,
                    )
            }

        assertDoesNotThrow { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should not accept request with empty body`() {
        val requestDto = PaymentMethodsRequest()

        assertThrows<NullPointerException> { service.searchPaymentMethods(requestDto, "test-id") }
    }

    @Test
    fun `should remap method management for cards method for old IO app device (null deviceVersion field)`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = null
            }

        val mockedResponse =
            PaymentMethodsResponseDto().apply {
                paymentMethods =
                    listOf(
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "card"
                            name = mapOf("it" to "Carta")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = "CP"
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                        },
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "paypal"
                            name = mapOf("it" to "paypal")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = "PPAL"
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.APP)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                        },
                    )
            }

        given(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .willReturn(Uni.createFrom().item(mockedResponse))

        val response =
            service.searchPaymentMethods(requestDto, "test-id").toCompletableFuture().get()

        assertEquals(2, response.paymentMethods?.size)
        val cardPaymentMethod = response.paymentMethods.first { it.paymentTypeCode == "CP" }
        val paypalPaymentMethod = response.paymentMethods.first { it.paymentTypeCode == "PPAL" }
        assertEquals(
            PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE_ONLY,
            cardPaymentMethod.methodManagement,
        )
        assertEquals(
            PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE,
            paypalPaymentMethod.methodManagement,
        )
    }

    @Test
    fun `should not remap method management for cards method for new IO app device (valued device version field)`() {
        val requestDto =
            PaymentMethodsRequest().apply {
                userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.IO
                userDevice = PaymentMethodsRequest.UserDeviceEnum.ANDROID
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                deviceVersion = "0.0.0"
            }

        val mockedResponse =
            PaymentMethodsResponseDto().apply {
                paymentMethods =
                    listOf(
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "card"
                            name = mapOf("it" to "Carta")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = "CP"
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                        },
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "paypal"
                            name = mapOf("it" to "paypal")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = "PPAL"
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.APP)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                        },
                    )
            }

        given(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .willReturn(Uni.createFrom().item(mockedResponse))

        val response =
            service.searchPaymentMethods(requestDto, "test-id").toCompletableFuture().get()

        assertEquals(2, response.paymentMethods?.size)
        val cardPaymentMethod = response.paymentMethods.first { it.paymentTypeCode == "CP" }
        val paypalPaymentMethod = response.paymentMethods.first { it.paymentTypeCode == "PPAL" }
        assertEquals(
            PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE,
            cardPaymentMethod.methodManagement,
        )
        assertEquals(
            PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE,
            paypalPaymentMethod.methodManagement,
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["0.0.0"])
    @NullSource
    fun `should skip method management remapping for device other than IO app ignoring device version field`(
        deviceVersion: String?
    ) {
        val requestDto =
            PaymentMethodsRequest().apply {
                this.userTouchpoint = PaymentMethodsRequest.UserTouchpointEnum.CHECKOUT
                this.userDevice = PaymentMethodsRequest.UserDeviceEnum.WEB
                this.totalAmount = 2500
                this.allCCp = true
                this.targetKey = "TAX_2025_RENDE"
                this.deviceVersion = deviceVersion
            }

        val mockedResponse =
            PaymentMethodsResponseDto().apply {
                paymentMethods =
                    listOf(
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "card"
                            name = mapOf("it" to "Carta")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = "CP"
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                        },
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "paypal"
                            name = mapOf("it" to "paypal")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = "PPAL"
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.APP)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                        },
                    )
            }

        given(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .willReturn(Uni.createFrom().item(mockedResponse))

        val response =
            service.searchPaymentMethods(requestDto, "test-id").toCompletableFuture().get()

        assertEquals(2, response.paymentMethods?.size)
        val cardPaymentMethod = response.paymentMethods.first { it.paymentTypeCode == "CP" }
        val paypalPaymentMethod = response.paymentMethods.first { it.paymentTypeCode == "PPAL" }
        assertEquals(
            PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE,
            cardPaymentMethod.methodManagement,
        )
        assertEquals(
            PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE,
            paypalPaymentMethod.methodManagement,
        )
    }

    // --- createSession helper methods ---

    private val testOrderId = "E1234567890123ab"

    private fun buildAfmPaymentMethodResponse(
        name: String = "CARDS",
        touchpoint: PaymentMethodResponseDto.UserTouchpointEnum =
            PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT,
    ): PaymentMethodResponseDto {
        return PaymentMethodResponseDto().apply {
            paymentMethodId = "pm-001"
            this.name = mapOf("it" to name)
            status = PaymentMethodResponseDto.StatusEnum.ENABLED
            group = "CP"
            methodManagement = PaymentMethodResponseDto.MethodManagementEnum.ONBOARDABLE
            paymentMethodAsset = "asset.png"
            userTouchpoint = listOf(touchpoint)
            paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
            validityDateFrom = LocalDate.now()
        }
    }

    private fun buildNpgFieldsDto(): NpgFieldsDto {
        return NpgFieldsDto(
            sessionId = "npg-session-123",
            securityToken = "npg-sec-token",
            fields =
                listOf(
                    NpgFieldDto(
                        "cardholderName",
                        "text",
                        "cardData",
                        "https://fe.npg.it/field.html?id=CARDHOLDER_NAME",
                    ),
                    NpgFieldDto(
                        "cardNumber",
                        "text",
                        "cardData",
                        "https://fe.npg.it/field.html?id=CARD_NUMBER",
                    ),
                ),
        )
    }

    private fun setupCreateSessionMocks(
        clientId: String = "CHECKOUT",
        touchpoint: PaymentMethodResponseDto.UserTouchpointEnum =
            PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT,
    ) {
        whenever(mockClient.getPaymentMethod(any(), any(), any()))
            .thenReturn(
                Uni.createFrom().item(buildAfmPaymentMethodResponse(touchpoint = touchpoint))
            )

        whenever(mockUniqueIdGenerator.generateUniqueId())
            .thenReturn(Uni.createFrom().item(testOrderId))

        whenever(mockJwtClient.createJWTToken(any()))
            .thenReturn(Uni.createFrom().item(CreateTokenResponse(token = "jwt-token")))

        whenever(mockSessionUrlConfig.basePath())
            .thenReturn(URI.create("https://checkout.pagopa.it"))
        whenever(mockSessionUrlConfig.ioBasePath()).thenReturn(URI.create("https://io.pagopa.it"))
        whenever(mockSessionUrlConfig.outcomeSuffix()).thenReturn("/esito")
        whenever(mockSessionUrlConfig.cancelSuffix()).thenReturn("/annulla")
        whenever(mockSessionUrlConfig.notificationUrl())
            .thenReturn(
                "https://api.pagopa.it/sessions/{orderId}/outcomes?sessionToken={sessionToken}"
            )

        whenever(mockNpgClient.buildForm(any()))
            .thenReturn(Uni.createFrom().item(buildNpgFieldsDto()))

        whenever(mockNpgSessionsRedis.save(any()))
            .thenReturn(
                Uni.createFrom()
                    .item(
                        NpgSessionDocument(testOrderId, "corr", "npg-session-123", "npg-sec-token")
                    )
            )
    }

    // --- createSession tests ---

    @Test
    fun `should create session for valid payment method`() {
        setupCreateSessionMocks()

        val result =
            service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()

        assertEquals(testOrderId, result.orderId)
        assertNotNull(result.correlationId)
        assertEquals("CARDS", result.paymentMethodData.paymentMethod)
        assertEquals(2, result.paymentMethodData.form.size)
        assertEquals("cardholderName", result.paymentMethodData.form[0].id)
        assertEquals("cardNumber", result.paymentMethodData.form[1].id)

        verify(mockClient).getPaymentMethod(any(), any(), any())
        verify(mockUniqueIdGenerator).generateUniqueId()
        verify(mockJwtClient).createJWTToken(any())
        verify(mockNpgClient).buildForm(any())
        verify(mockNpgSessionsRedis).save(any())
    }

    @Test
    fun `should create session for IO client using io base path`() {
        setupCreateSessionMocks(
            clientId = "IO",
            touchpoint = PaymentMethodResponseDto.UserTouchpointEnum.IO,
        )

        val result =
            service.createSessionForPaymentMethod("pm-001", null, "IO").await().indefinitely()

        assertNotNull(result)
        assertEquals(testOrderId, result.orderId)

        // Verify that ioBasePath was used (not basePath)
        verify(mockSessionUrlConfig).ioBasePath()
    }

    @Test
    fun `should create session for CHECKOUT client using checkout base path`() {
        setupCreateSessionMocks()

        service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()

        verify(mockSessionUrlConfig).basePath()
    }

    @Test
    fun `should fail with JwtIssuerResponseException when JWT creation fails`() {
        whenever(mockClient.getPaymentMethod(any(), any(), any()))
            .thenReturn(Uni.createFrom().item(buildAfmPaymentMethodResponse()))

        whenever(mockUniqueIdGenerator.generateUniqueId())
            .thenReturn(Uni.createFrom().item(testOrderId))

        whenever(mockJwtClient.createJWTToken(any()))
            .thenReturn(
                Uni.createFrom()
                    .failure(
                        JwtIssuerResponseException(
                            "error jwtIssuer",
                            RuntimeException("conn error"),
                        )
                    )
            )

        assertThrows<JwtIssuerResponseException> {
            service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()
        }

        verify(mockNpgClient, times(0)).buildForm(any())
        verify(mockNpgSessionsRedis, times(0)).save(any())
    }

    @Test
    fun `should fail with PaymentMethodNotFoundException when payment method does not exist`() {
        whenever(mockClient.getPaymentMethod(any(), any(), any()))
            .thenReturn(
                Uni.createFrom().failure(PaymentMethodNotFoundException("Payment method not found"))
            )

        assertThrows<PaymentMethodNotFoundException> {
            service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()
        }

        verify(mockUniqueIdGenerator, times(0)).generateUniqueId()
        verify(mockJwtClient, times(0)).createJWTToken(any())
    }

    @Test
    fun `should default to CHECKOUT when xClientId is null`() {
        setupCreateSessionMocks()

        service.createSessionForPaymentMethod("pm-001", null, null).await().indefinitely()

        verify(mockClient).getPaymentMethod(any(), any(), org.mockito.kotlin.eq("CHECKOUT"))
    }

    @Test
    fun `should save session to Redis with correct data`() {
        setupCreateSessionMocks()

        service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()

        verify(mockNpgSessionsRedis)
            .save(
                org.mockito.kotlin.argThat {
                    this.orderId == testOrderId &&
                        this.sessionId == "npg-session-123" &&
                        this.securityToken == "npg-sec-token"
                }
            )
    }

    @Test
    fun `should pass language to NPG buildForm`() {
        setupCreateSessionMocks()

        service.createSessionForPaymentMethod("pm-001", "it", "CHECKOUT").await().indefinitely()

        verify(mockNpgClient).buildForm(org.mockito.kotlin.argThat { this.language == "it" })
    }

    @Test
    fun `should pass correct claims to JWT token creation`() {
        setupCreateSessionMocks()

        service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()

        verify(mockJwtClient)
            .createJWTToken(
                org.mockito.kotlin.argThat {
                    this.privateClaims["orderId"] == testOrderId &&
                        this.privateClaims["paymentMethodId"] == "pm-001" &&
                        this.audience == "npg" &&
                        this.duration == 900
                }
            )
    }

    @Test
    fun `should handle null name map in payment method response`() {
        val pmResponse =
            PaymentMethodResponseDto().apply {
                paymentMethodId = "pm-001"
                name = null
                status = PaymentMethodResponseDto.StatusEnum.ENABLED
                group = "CP"
                methodManagement = PaymentMethodResponseDto.MethodManagementEnum.ONBOARDABLE
                paymentMethodAsset = "asset.png"
                userTouchpoint = listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
                validityDateFrom = LocalDate.now()
            }

        whenever(mockClient.getPaymentMethod(any(), any(), any()))
            .thenReturn(Uni.createFrom().item(pmResponse))

        whenever(mockUniqueIdGenerator.generateUniqueId())
            .thenReturn(Uni.createFrom().item(testOrderId))

        assertThrows<IllegalArgumentException> {
            service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()
        }
    }

    @Test
    fun `should handle empty name map in payment method response`() {
        val pmResponse =
            PaymentMethodResponseDto().apply {
                paymentMethodId = "pm-001"
                name = emptyMap()
                status = PaymentMethodResponseDto.StatusEnum.ENABLED
                group = "CP"
                methodManagement = PaymentMethodResponseDto.MethodManagementEnum.ONBOARDABLE
                paymentMethodAsset = "asset.png"
                userTouchpoint = listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
                validityDateFrom = LocalDate.now()
            }

        whenever(mockClient.getPaymentMethod(any(), any(), any()))
            .thenReturn(Uni.createFrom().item(pmResponse))

        whenever(mockUniqueIdGenerator.generateUniqueId())
            .thenReturn(Uni.createFrom().item(testOrderId))

        assertThrows<IllegalArgumentException> {
            service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()
        }
    }

    @Test
    fun `should handle field with null src`() {
        setupCreateSessionMocks()

        // Override NPG response with a field that has null src
        whenever(mockNpgClient.buildForm(any()))
            .thenReturn(
                Uni.createFrom()
                    .item(
                        it.pagopa.ecommerce.payment.methods.client.NpgFieldsDto(
                            sessionId = "npg-session-123",
                            securityToken = "npg-sec-token",
                            fields =
                                listOf(
                                    it.pagopa.ecommerce.payment.methods.client.NpgFieldDto(
                                        id = "cardholderName",
                                        type = "text",
                                        propertyClass = "cardData",
                                        src = null,
                                    )
                                ),
                        )
                    )
            )

        val result =
            service.createSessionForPaymentMethod("pm-001", null, "CHECKOUT").await().indefinitely()

        assertEquals(1, result.paymentMethodData.form.size)
        assertEquals(null, result.paymentMethodData.form[0].src)
    }
}
