package it.pagopa.ecommerce.payment.methods.services

import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.TestUtils
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.exception.NoBundleFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.utils.BundleOptions
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.generated.ecommerce.client.api.CalculatorApi
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.BundleOptionDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import it.pagopa.generated.ecommerce.client.model.TransferDto
import jakarta.ws.rs.core.Response
import java.time.LocalDate
import kotlin.test.assertTrue
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever

@QuarkusTest
class PaymentMethodsClientTest {

    private val mockClient = Mockito.mock(PaymentMethodsClient::class.java)
    private val service = PaymentMethodServiceImpl(mockClient)

    private val mockApi = Mockito.mock(PaymentMethodsApi::class.java)
    private val calculatorMockApi = Mockito.mock(CalculatorApi::class.java)
    private val client = PaymentMethodsClient(mockApi, calculatorMockApi)

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

    @Test
    fun `should delegate calculateFees to client`() {
        val expectedResponseDto = TestUtils.buildCalculateFeeResponse()

        whenever(
                mockClient.calculateFees(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Uni.createFrom().item(expectedResponseDto))

        val result =
            service
                .calculateFees(
                    "test-id",
                    TestUtils.buildCalculateFeeRequest(),
                    "req-id",
                    "CHECKOUT",
                    "IT",
                    Int.MAX_VALUE,
                )
                .toCompletableFuture()
                .get()

        assertEquals(expectedResponseDto, result)
    }

    @Test
    fun `should propagate exception from client on calculateFees`() {
        val expectedException = PaymentMethodsClientException("error")

        whenever(
                mockClient.calculateFees(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Uni.createFrom().failure(expectedException))

        assertThrows<Exception> {
            service
                .calculateFees(
                    "test-id",
                    TestUtils.buildCalculateFeeRequest(),
                    "req-id",
                    "CHECKOUT",
                    "IT",
                    Int.MAX_VALUE,
                )
                .toCompletableFuture()
                .get()
        }
    }

    @Test
    fun `should pass INT_MAX_VALUE as maxOccurrences to client`() {
        val captor = argumentCaptor<Int>()
        val expectedResponseDto = TestUtils.buildCalculateFeeResponse()

        whenever(
                mockClient.calculateFees(
                    anyOrNull(),
                    anyOrNull(),
                    captor.capture(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Uni.createFrom().item(expectedResponseDto))

        service
            .calculateFees(
                "test-id",
                TestUtils.buildCalculateFeeRequest(),
                "req-id",
                "CHECKOUT",
                "IT",
                Int.MAX_VALUE,
            )
            .toCompletableFuture()
            .get()

        assertEquals(Int.MAX_VALUE, captor.firstValue)
    }

    @Test
    fun `should throw NoBundleFoundException when no bundles are returned`() {
        val methodId = "test-id"

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(
                Uni.createFrom().item {
                    PaymentMethodResponseDto().apply {
                        paymentMethodId = methodId
                        userTouchpoint =
                            listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                        status = PaymentMethodResponseDto.StatusEnum.ENABLED
                        group = "CP"
                    }
                }
            )

        whenever(
                calculatorMockApi.getFeesMulti(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(
                Uni.createFrom().item { BundleOptionDto().apply { bundleOptions = emptyList() } }
            )

        assertThrows<NoBundleFoundException> {
            client
                .calculateFees(
                    methodId,
                    TestUtils.buildCalculateFeeRequest(),
                    10,
                    "test-id",
                    "CHECKOUT",
                    "IT",
                )
                .await()
                .indefinitely()
        }
    }

    @Test
    fun `should remove duplicate psp bundles and return response`() {
        val methodId = "test-id"
        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(
                Uni.createFrom().item {
                    PaymentMethodResponseDto().apply {
                        paymentMethodId = methodId
                        userTouchpoint =
                            listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                        status = PaymentMethodResponseDto.StatusEnum.ENABLED
                        group = "CP"
                        name = mapOf("IT" to "Carte")
                        description = mapOf("IT" to "Descrizione")
                        paymentMethodAsset = "asset"
                        paymentMethodsBrandAssets = mapOf("brand" to "asset")
                    }
                }
            )

        whenever(
                calculatorMockApi.getFeesMulti(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(
                Uni.createFrom().item {
                    BundleOptionDto().apply {
                        bundleOptions =
                            listOf(
                                TransferDto().apply {
                                    idPsp = "psp-1"
                                    taxPayerFee = 100L
                                    bundleName = "bundle"
                                    abi = "abi"
                                    idBundle = "bundle-id"
                                    idChannel = "channel"
                                    idBrokerPsp = "broker"
                                    onUs = false
                                    touchpoint = "CHECKOUT"
                                    pspBusinessName = "PSP Name"
                                },
                                TransferDto().apply {
                                    idPsp = "psp-1"
                                    taxPayerFee = 100L
                                    bundleName = "bundle"
                                    abi = "abi"
                                    idBundle = "bundle-id"
                                    idChannel = "channel"
                                    idBrokerPsp = "broker"
                                    onUs = false
                                    touchpoint = "CHECKOUT"
                                    pspBusinessName = "PSP Name"
                                },
                                TransferDto().apply {
                                    idPsp = "psp-2"
                                    taxPayerFee = 100L
                                    bundleName = "bundle"
                                    abi = "abi"
                                    idBundle = "bundle-id"
                                    idChannel = "channel"
                                    idBrokerPsp = "broker"
                                    onUs = false
                                    touchpoint = "CHECKOUT"
                                    pspBusinessName = "PSP Name"
                                },
                            )
                    }
                }
            )
        val response =
            client
                .calculateFees(
                    methodId,
                    TestUtils.buildCalculateFeeRequest(),
                    10,
                    "test-id",
                    "CHECKOUT",
                    "IT",
                )
                .await()
                .indefinitely()

        assertNotNull(response)
        assertEquals(2, response.bundles?.size)
    }

    @Test
    fun `should throw NoBundleFoundException with cause when no bundles are returned`() {
        val methodId = "test-id"
        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(
                Uni.createFrom().item {
                    PaymentMethodResponseDto().apply {
                        paymentMethodId = methodId
                        userTouchpoint =
                            listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                        status = PaymentMethodResponseDto.StatusEnum.ENABLED
                        group = "CP"
                    }
                }
            )

        whenever(
                calculatorMockApi.getFeesMulti(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(
                Uni.createFrom().item { BundleOptionDto().apply { bundleOptions = emptyList() } }
            )

        val thrown =
            assertThrows<NoBundleFoundException> {
                client
                    .calculateFees(
                        methodId,
                        TestUtils.buildCalculateFeeRequest(),
                        10,
                        "test-id",
                        "CHECKOUT",
                        "IT",
                    )
                    .await()
                    .indefinitely()
            }
        assertNotNull(thrown.message)
    }

    @Test
    fun `should throw PaymentMethodNotFoundException when getPaymentMethod returns 404 during calculateFees`() {
        val methodId = "test-id"
        val mockResponse = ClientResponseImpl()
        mockResponse.setStatus(Response.Status.NOT_FOUND.statusCode)

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().failure(ClientWebApplicationException(mockResponse)))

        assertThrows<PaymentMethodNotFoundException> {
            client
                .calculateFees(
                    methodId,
                    TestUtils.buildCalculateFeeRequest(),
                    10,
                    "test-id",
                    "CHECKOUT",
                    "IT",
                )
                .await()
                .indefinitely()
        }
    }

    @Test
    fun `should throw PaymentMethodsClientException when getPaymentMethod fails during calculateFees`() {
        val methodId = "test-id"

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().failure(RuntimeException("generic error")))

        assertThrows<PaymentMethodsClientException> {
            client
                .calculateFees(
                    methodId,
                    TestUtils.buildCalculateFeeRequest(),
                    10,
                    "test-id",
                    "CHECKOUT",
                    "IT",
                )
                .await()
                .indefinitely()
        }
    }

    @Test
    fun `should throw PaymentMethodNotFoundException when payment method does not support client id during calculateFees`() {
        val methodId = "test-id"

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(
                Uni.createFrom().item {
                    PaymentMethodResponseDto().apply {
                        paymentMethodId = methodId
                        userTouchpoint =
                            listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                        status = PaymentMethodResponseDto.StatusEnum.ENABLED
                        group = "CP"
                        name = mapOf("IT" to "Carte")
                        description = mapOf("IT" to "Descrizione")
                        paymentMethodAsset = "asset"
                        paymentMethodsBrandAssets = mapOf("brand" to "asset")
                    }
                }
            )

        assertThrows<PaymentMethodNotFoundException> {
            client
                .calculateFees(
                    methodId,
                    TestUtils.buildCalculateFeeRequest(),
                    10,
                    "test-id",
                    "IO",
                    "IT",
                )
                .await()
                .indefinitely()
        }
    }

    @Test
    fun `should return empty list when bundleOptions is null`() {
        val bundle = BundleOptionDto()
        bundle.bundleOptions = null

        val result = BundleOptions.removeDuplicatePsp(bundle)

        assertTrue(result.bundleOptions!!.isEmpty())
    }

    @Test
    fun `should create NoBundleFoundException with cause`() {
        val cause = RuntimeException("original cause")
        val exception = NoBundleFoundException("test message", cause)

        assertEquals("test message", exception.message)
        assertEquals(cause, exception.cause)
    }
}
