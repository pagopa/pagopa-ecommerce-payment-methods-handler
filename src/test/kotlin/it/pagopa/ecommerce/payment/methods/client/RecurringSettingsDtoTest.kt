package it.pagopa.ecommerce.payment.methods.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecurringSettingsDtoTest {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    // --- RecurringAction enum ---

    @Test
    fun `RecurringAction should contain all expected values`() {
        val expected =
            setOf("NO_RECURRING", "SUBSEQUENT_PAYMENT", "CONTRACT_CREATION", "CARD_SUBSTITUTION")
        assertEquals(expected, RecurringAction.entries.map { it.name }.toSet())
    }

    @Test
    fun `RecurringAction valueOf should resolve all entries`() {
        assertEquals(RecurringAction.NO_RECURRING, RecurringAction.valueOf("NO_RECURRING"))
        assertEquals(
            RecurringAction.SUBSEQUENT_PAYMENT,
            RecurringAction.valueOf("SUBSEQUENT_PAYMENT"),
        )
        assertEquals(
            RecurringAction.CONTRACT_CREATION,
            RecurringAction.valueOf("CONTRACT_CREATION"),
        )
        assertEquals(
            RecurringAction.CARD_SUBSTITUTION,
            RecurringAction.valueOf("CARD_SUBSTITUTION"),
        )
    }

    // --- RecurringContractType enum ---

    @Test
    fun `RecurringContractType should contain all expected values`() {
        val expected = setOf("MIT_UNSCHEDULED", "MIT_SCHEDULED", "CIT")
        assertEquals(expected, RecurringContractType.entries.map { it.name }.toSet())
    }

    @Test
    fun `RecurringContractType valueOf should resolve all entries`() {
        assertEquals(
            RecurringContractType.MIT_UNSCHEDULED,
            RecurringContractType.valueOf("MIT_UNSCHEDULED"),
        )
        assertEquals(
            RecurringContractType.MIT_SCHEDULED,
            RecurringContractType.valueOf("MIT_SCHEDULED"),
        )
        assertEquals(RecurringContractType.CIT, RecurringContractType.valueOf("CIT"))
    }

    // --- RecurringSettingsDto data class ---

    @Test
    fun `RecurringSettingsDto should default all fields to null`() {
        val dto = RecurringSettingsDto()
        assertNull(dto.action)
        assertNull(dto.contractId)
        assertNull(dto.contractType)
        assertNull(dto.contractExpiryDate)
        assertNull(dto.contractFrequency)
    }

    @Test
    fun `RecurringSettingsDto should hold all fields correctly`() {
        val dto =
            RecurringSettingsDto(
                action = RecurringAction.SUBSEQUENT_PAYMENT,
                contractId = "C2834987",
                contractType = RecurringContractType.CIT,
                contractExpiryDate = "2023-03-16",
                contractFrequency = "120",
            )
        assertEquals(RecurringAction.SUBSEQUENT_PAYMENT, dto.action)
        assertEquals("C2834987", dto.contractId)
        assertEquals(RecurringContractType.CIT, dto.contractType)
        assertEquals("2023-03-16", dto.contractExpiryDate)
        assertEquals("120", dto.contractFrequency)
    }

    @Test
    fun `RecurringSettingsDto copy should work correctly`() {
        val original =
            RecurringSettingsDto(
                action = RecurringAction.CONTRACT_CREATION,
                contractType = RecurringContractType.MIT_SCHEDULED,
            )
        val copy = original.copy(contractId = "NEW-ID", contractFrequency = "30")
        assertEquals(RecurringAction.CONTRACT_CREATION, copy.action)
        assertEquals("NEW-ID", copy.contractId)
        assertEquals(RecurringContractType.MIT_SCHEDULED, copy.contractType)
        assertNull(copy.contractExpiryDate)
        assertEquals("30", copy.contractFrequency)
    }

    @Test
    fun `RecurringSettingsDto equals and hashCode should work`() {
        val dto1 =
            RecurringSettingsDto(
                action = RecurringAction.NO_RECURRING,
                contractType = RecurringContractType.CIT,
            )
        val dto2 =
            RecurringSettingsDto(
                action = RecurringAction.NO_RECURRING,
                contractType = RecurringContractType.CIT,
            )
        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
    }

    // --- JSON serialization ---

    @Test
    fun `RecurringSettingsDto should serialize to JSON correctly`() {
        val dto =
            RecurringSettingsDto(
                action = RecurringAction.SUBSEQUENT_PAYMENT,
                contractId = "C123",
                contractType = RecurringContractType.CIT,
            )
        val json = objectMapper.writeValueAsString(dto)
        val tree = objectMapper.readTree(json)
        assertEquals("SUBSEQUENT_PAYMENT", tree["action"].asText())
        assertEquals("C123", tree["contractId"].asText())
        assertEquals("CIT", tree["contractType"].asText())
        assertTrue(tree["contractExpiryDate"].isNull)
        assertTrue(tree["contractFrequency"].isNull)
    }

    @Test
    fun `RecurringSettingsDto should deserialize from JSON correctly`() {
        val json =
            """
            {
                "action": "CARD_SUBSTITUTION",
                "contractId": "C999",
                "contractType": "MIT_UNSCHEDULED",
                "contractExpiryDate": "2025-12-31",
                "contractFrequency": "60"
            }
            """
                .trimIndent()
        val dto = objectMapper.readValue<RecurringSettingsDto>(json)
        assertEquals(RecurringAction.CARD_SUBSTITUTION, dto.action)
        assertEquals("C999", dto.contractId)
        assertEquals(RecurringContractType.MIT_UNSCHEDULED, dto.contractType)
        assertEquals("2025-12-31", dto.contractExpiryDate)
        assertEquals("60", dto.contractFrequency)
    }

    @Test
    fun `RecurringSettingsDto should deserialize null fields from JSON`() {
        val json = """{}"""
        val dto = objectMapper.readValue<RecurringSettingsDto>(json)
        assertNull(dto.action)
        assertNull(dto.contractId)
        assertNull(dto.contractType)
        assertNull(dto.contractExpiryDate)
        assertNull(dto.contractFrequency)
    }

    // --- NpgPaymentSessionDto integration ---

    @Test
    fun `NpgPaymentSessionDto should serialize recurrence as typed object`() {
        val session =
            NpgPaymentSessionDto(
                paymentService = "CARDS",
                resultUrl = "https://example.com/result",
                cancelUrl = "https://example.com/cancel",
                notificationUrl = "https://example.com/notify",
                recurrence =
                    RecurringSettingsDto(
                        action = RecurringAction.SUBSEQUENT_PAYMENT,
                        contractId = "C123",
                        contractType = RecurringContractType.CIT,
                    ),
            )
        val json = objectMapper.writeValueAsString(session)
        val tree = objectMapper.readTree(json)
        val recurrenceNode = tree["recurrence"]
        assertEquals("SUBSEQUENT_PAYMENT", recurrenceNode["action"].asText())
        assertEquals("C123", recurrenceNode["contractId"].asText())
        assertEquals("CIT", recurrenceNode["contractType"].asText())
    }

    @Test
    fun `NpgPaymentSessionDto should serialize null recurrence correctly`() {
        val session =
            NpgPaymentSessionDto(
                paymentService = "CARDS",
                resultUrl = "https://example.com/result",
                cancelUrl = "https://example.com/cancel",
                notificationUrl = "https://example.com/notify",
            )
        val json = objectMapper.writeValueAsString(session)
        val tree = objectMapper.readTree(json)
        assertTrue(tree["recurrence"].isNull)
    }
}
