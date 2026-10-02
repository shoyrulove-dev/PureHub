package com.purehub.app.feature.wifi

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WifiMonitorStoreTest {
    @Test
    fun `active foreground session persists and becomes a report`() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("monitor-store-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val session = WifiMonitorSession(
            startedAt = 100,
            endsAt = 10_000,
            intervalSeconds = 30,
            samples = listOf(WifiMonitorSample(200, -62, "AA:BB", true, true, 24)),
            events = listOf(WifiMonitorEvent(300, "Roaming", "AA -> BB")),
        )

        WifiMonitorStore.saveActive(preferences, session)
        assertEquals(30, WifiMonitorStore.loadActive(preferences)?.intervalSeconds)
        val report = WifiMonitorStore.finishActive(preferences, 9_000)

        assertEquals(100, report?.uptimePercent)
        assertNull(WifiMonitorStore.loadActive(preferences))
        assertTrue(WifiMonitorStore.load(preferences).isNotEmpty())
    }
}
