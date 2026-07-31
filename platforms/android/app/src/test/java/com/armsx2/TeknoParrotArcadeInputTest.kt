package com.armsx2

import com.armsx2.ui.touch.TeknoParrotArcadeInput
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeknoParrotArcadeInputTest {
    @Test
    fun technicBeatUsesButtonCabinetForSharedIdentifier() {
        assertFalse(
            TeknoParrotArcadeInput.isLightgunGame(
                "NM00003",
                "technicb",
            ),
        )
    }

    @Test
    fun vampireNightRetainsLightgunCabinetForSharedIdentifier() {
        assertTrue(
            TeknoParrotArcadeInput.isLightgunGame(
                "NM00003",
                "vnight",
            ),
        )
    }

    @Test
    fun legacyLaunchWithoutProfileKeepsExistingClassification() {
        assertTrue(TeknoParrotArcadeInput.isLightgunGame("NM00003"))
    }
}
