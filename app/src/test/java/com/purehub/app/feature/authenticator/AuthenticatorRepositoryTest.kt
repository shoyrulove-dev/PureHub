package com.purehub.app.feature.authenticator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatorRepositoryTest {
    @Test
    fun codecRoundTripPreservesGroups() {
        val accounts = listOf(TotpAccount(1, "Email", "JBSWY3DPEHPK3PXP", "Work"))
        assertEquals(accounts, AuthenticatorCodec.decode(AuthenticatorCodec.encode(accounts)))
    }

    @Test
    fun oldPayloadDefaultsToPersonalGroupAndMalformedFailsClosed() {
        val old = """[{"id":1,"label":"Email","secret":"ABC"}]"""
        assertEquals("Personal", AuthenticatorCodec.decode(old).single().group)
        assertTrue(AuthenticatorCodec.decode("not-json").isEmpty())
    }
}
