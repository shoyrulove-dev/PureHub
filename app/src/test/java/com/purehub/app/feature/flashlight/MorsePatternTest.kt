package com.purehub.app.feature.flashlight

import org.junit.Assert.assertEquals
import org.junit.Test

class MorsePatternTest {
    @Test fun singleLetterHasNoTrailingGap() {
        assertEquals(listOf(160L, 160L, 480L), buildMorsePattern("A"))
    }

    @Test fun spacesExtendOffTimeWithoutChangingOnOffParity() {
        assertEquals(listOf(160L, 1120L, 160L), buildMorsePattern("E   E"))
        assertEquals(listOf(160L, 480L, 160L), buildMorsePattern("EE"))
    }

    @Test fun sosHasNineSignalsAndCorrectLetterGaps() {
        val pattern = buildMorsePattern("SOS")
        assertEquals(17, pattern.size)
        assertEquals(listOf(160L, 160L, 160L, 480L, 480L, 480L, 160L, 160L, 160L), pattern.filterIndexed { i, _ -> i % 2 == 0 })
        assertEquals(480L, pattern[5])
        assertEquals(480L, pattern[11])
    }

    @Test fun unsupportedInputAndWhitespaceAreSafe() {
        assertEquals(emptyList<Long>(), buildMorsePattern(" 123! "))
        assertEquals(buildMorsePattern("HELP"), buildMorsePattern(" help! "))
    }
}
