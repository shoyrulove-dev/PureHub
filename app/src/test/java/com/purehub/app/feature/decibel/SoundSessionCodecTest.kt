package com.purehub.app.feature.decibel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundSessionCodecTest {
    @Test
    fun sessionRoundTripAndCsvArePortable() {
        val session = SoundSession(10, 20, 30f, 40f, 50f, listOf(SoundPoint(10, 40.5f)))
        assertEquals(session, SoundSessionCodec.decode(SoundSessionCodec.encode(listOf(session))).single())
        assertTrue(SoundSessionCodec.csv(session).contains("10,40.5"))
    }

    @Test
    fun malformedHistoryFailsClosed() {
        assertTrue(SoundSessionCodec.decode("broken").isEmpty())
    }
}
