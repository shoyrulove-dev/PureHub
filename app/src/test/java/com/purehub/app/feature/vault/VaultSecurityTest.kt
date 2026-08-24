package com.purehub.app.feature.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSecurityTest {
    @Test
    fun generatedPasswordIsStrongAndRequestedLength() {
        val password = VaultSecurity.generate(24)
        assertEquals(24, password.length)
        assertFalse(VaultSecurity.isWeak(password))
    }

    @Test
    fun healthFlagsWeakAndReusedEntries() {
        val entries = listOf(
            VaultEntry("1", "One", "a", "password"),
            VaultEntry("2", "Two", "b", "password"),
            VaultEntry("3", "Three", "c", "Long_Strong-Password-47"),
        )
        val health = VaultSecurity.health(entries)
        assertEquals(2, health.weak)
        assertEquals(2, health.reused)
        assertTrue(health.score < 100)
    }
}
