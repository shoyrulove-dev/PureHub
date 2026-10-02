package com.purehub.app.feature.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanDiscoveryTest {
    @Test
    fun parsesIndividualPortsAndRangesWithoutDuplicates() {
        assertEquals(
            listOf(22, 80, 443, 8000, 8001, 8002),
            LanDiscovery.parsePortSpecification("22, 80,443,8000-8002,80"),
        )
    }

    @Test
    fun rejectsInvalidPortsAndBoundsLargeRanges() {
        val ports = LanDiscovery.parsePortSpecification("0,65536,nope,1-1000", maxPorts = 32)

        assertEquals((1..32).toList(), ports)
    }

    @Test
    fun parsesSsdpServerLocationAndDiscoverySource() {
        val response = """HTTP/1.1 200 OK
            |LOCATION: http://192.168.1.42:1400/xml/device_description.xml
            |SERVER: Linux UPnP/1.0 Sonos/80.1
            |ST: urn:schemas-upnp-org:device:MediaRenderer:1
            |USN: uuid:test::urn:schemas-upnp-org:device:MediaRenderer:1
        """.trimMargin()

        val device = LanDiscovery.parseSsdpResponse(response, "192.168.1.42")

        assertEquals("192.168.1.42", device.ipAddress)
        assertEquals("Linux UPnP", device.hostName)
        assertTrue("SSDP/UPnP" in device.discoverySources)
        assertTrue(device.services.any { it.contains("MediaRenderer") })
    }
}
