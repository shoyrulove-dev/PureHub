package com.purehub.app.feature.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordVaultStateTest {
    @Test
    fun vaultEntryHoldsExpectedFields() {
        val entry = VaultEntry(
            id = "1",
            title = "Email",
            username = "user@example.com",
            password = "secret123",
        )

        assertEquals("Email", entry.title)
        assertEquals("user@example.com", entry.username)
        assertEquals("secret123", entry.password)
    }

    @Test
    fun malformedVaultDataFailsClosed() {
        assertTrue(VaultEntryCodec.decode("not-json").isEmpty())
        assertTrue(VaultEntryCodec.decode("[null,{},42]").isEmpty())
    }

    @Test
    fun codecRoundTripPreservesEncryptedPreferencePayload() {
        val entries = listOf(VaultEntry("id-1", "Email", "person@example.com", "long-secret"))
        assertEquals(entries, VaultEntryCodec.decode(VaultEntryCodec.encode(entries)))
    }
}
