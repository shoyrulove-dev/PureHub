package com.purehub.app.feature.vault

import org.junit.Assert.assertEquals
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
}
