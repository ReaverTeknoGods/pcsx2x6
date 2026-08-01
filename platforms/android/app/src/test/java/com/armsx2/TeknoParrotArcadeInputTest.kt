package com.armsx2

import com.armsx2.ui.touch.TeknoParrotArcadeInput
import com.armsx2.ui.touch.actionButtonsFor
import android.view.KeyEvent
import org.junit.Assert.assertEquals
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

    @Test
    fun allTeknoParrotTaikoRevisionsUseTheDrumCabinet() {
        val taikoIds = setOf(
            "NM00023", "NM00033", "NM00038", "NM00041",
            "NM00044", "NM00051", "NM00056", "NM00057",
        )

        taikoIds.forEach { gameId ->
            assertTrue("$gameId must use the drum surface", TeknoParrotArcadeInput.isDrumGame(gameId))
            assertTrue("$gameId must not show generic action buttons", actionButtonsFor(gameId).isEmpty())
            assertEquals(
                TeknoParrotArcadeInput.AXIS_DRUM_P1_LEFT_CENTER,
                TeknoParrotArcadeInput.gamepadDrumAxis(gameId, KeyEvent.KEYCODE_BUTTON_X),
            )
        }
    }

    @Test
    fun motoGpUsesTheCompleteDrivingCabinet() {
        assertTrue(TeknoParrotArcadeInput.isDrivingGame("NM00039"))
        assertEquals(
            listOf("ENTER", "VIEW", "GEAR+", "GEAR-"),
            actionButtonsFor("NM00039").map { it.label },
        )
        assertEquals(
            TeknoParrotArcadeInput.BUTTON_2,
            TeknoParrotArcadeInput.gamepadButton("NM00039", KeyEvent.KEYCODE_BUTTON_Y),
        )
        assertEquals(
            TeknoParrotArcadeInput.BUTTON_4,
            TeknoParrotArcadeInput.gamepadButton("NM00039", KeyEvent.KEYCODE_BUTTON_L1),
        )
        assertEquals(
            TeknoParrotArcadeInput.BUTTON_3,
            TeknoParrotArcadeInput.gamepadButton("NM00039", KeyEvent.KEYCODE_BUTTON_R1),
        )
    }

    @Test
    fun lightgunCabinetsExposeTheirActualMenuAndAuxiliarySwitches() {
        assertEquals(
            "START",
            TeknoParrotArcadeInput.lightgunAuxiliaryLabel("NM00003", "vnight"),
        )
        assertEquals(
            TeknoParrotArcadeInput.BUTTON_3,
            TeknoParrotArcadeInput.lightgunAuxiliaryButton("NM00003", "vnight"),
        )
        assertEquals(
            TeknoParrotArcadeInput.BUTTON_3,
            TeknoParrotArcadeInput.gamepadButton(
                "NM00003", KeyEvent.KEYCODE_BUTTON_START, "vnight",
            ),
        )
        assertEquals(
            TeknoParrotArcadeInput.BUTTON_6,
            TeknoParrotArcadeInput.lightgunAuxiliaryButton("NM00012"),
        )
        listOf("NM00021", "NM00032").forEach { gameId ->
            assertEquals(
                TeknoParrotArcadeInput.LEFT,
                TeknoParrotArcadeInput.gamepadButton(gameId, KeyEvent.KEYCODE_BUTTON_Y),
            )
            assertEquals(
                TeknoParrotArcadeInput.BUTTON_3,
                TeknoParrotArcadeInput.lightgunAuxiliaryButton(gameId),
            )
        }
    }

    @Test
    fun supportedCatalogHasAnAuditedActionLayout() {
        val noActionSurface = setOf(
            "NM00012", "NM00021", "NM00023", "NM00032", "NM00033", "NM00038",
            "NM00041", "NM00044", "NM00051", "NM00056", "NM00057",
        )
        val threeButtonSurface = setOf("NM00003", "NM00035")
        val sixButtonSurface = setOf("NM00009", "NM00018")

        TeknoParrotArcadeInput.supportedGameIds.forEach { gameId ->
            val count = if (gameId in noActionSurface) 0 else actionButtonsFor(gameId).size
            when {
                gameId in noActionSurface -> assertEquals("$gameId", 0, count)
                gameId in threeButtonSurface -> assertEquals("$gameId", 3, count)
                gameId in sixButtonSurface -> assertEquals("$gameId", 6, count)
                else -> assertTrue("$gameId has no audited action layout", count in 2..4)
            }
        }
    }
}
