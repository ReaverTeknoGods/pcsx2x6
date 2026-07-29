package com.armsx2.ui.touch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import android.view.KeyEvent
import com.armsx2.EmuState
import com.armsx2.runtime.MainActivityRuntime
import com.armsx2.ui.WindowImpl
import kr.co.iefriends.pcsx2.NativeApp
import kotlin.math.abs
import kotlin.math.min

/**
 * TeknoParrot's arcade controls for PCSX2X6 companion sessions.
 *
 * This intentionally does not translate touch input into a DualShock pad. The
 * controls feed the same JVS state merged with TPUI's TPJ1 shared page, keeping
 * touch and forwarded physical controllers on one cabinet-accurate input path.
 * The positions mirror TeknoParrot's existing "Arcade Stick" Winlator profile.
 */
@Composable
fun TeknoParrotArcadeControlsOverlay() {
    // Load the persisted visibility preference before deciding whether the
    // TP arcade surface should be drawn. The stock overlay does this itself,
    // but companion mode replaces that composable entirely.
    TouchControls.ensureLoaded()
    val gameId = MainActivityRuntime.teknoParrotGameId
    val running = MainActivityRuntime.eState.value == EmuState.RUNNING ||
        MainActivityRuntime.eState.value == EmuState.PAUSED
    if (!running ||
        TouchControls.visibilityMode.intValue == 0 ||
        !TouchControls.visible.value ||
        WindowImpl.overlayVisible.value ||
        WindowImpl.showLibrary.value ||
        WindowImpl.inGameScreen.value != null
    ) {
        return
    }

    DisposableEffect(gameId) {
        onDispose { NativeApp.clearTeknoParrotOverlayInput() }
    }

    BoxWithConstraints {
        val driving = TeknoParrotArcadeInput.isDrivingGame(gameId)
        val lightgun = TeknoParrotArcadeInput.isLightgunGame(gameId)
        val battleGear = TeknoParrotArcadeInput.isBattleGearGame(gameId)
        val zoids = TeknoParrotArcadeInput.isZoidsGame(gameId)
        val drum = TeknoParrotArcadeInput.isDrumGame(gameId)
        val shortSide = minOf(maxWidth, maxHeight)
        val dpadSize = shortSide * 0.37f
        val actionSize = shortSide * if (driving) 0.115f else 0.155f
        val menuWidth = shortSide * 0.18f
        val menuHeight = shortSide * 0.085f
        val utilityWidth = shortSide * 0.16f
        val utilityHeight = shortSide * 0.068f

        if (lightgun) {
            val aimWidth = maxWidth * 0.64f
            val aimHeight = maxHeight * 0.68f
            val pedalSize = shortSide * 0.13f
            LightgunAimSurface(
                gameId,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.50f, 0.46f,
                    aimWidth, aimHeight,
                ),
            )
            ArcadeButton(
                "PEDAL",
                TeknoParrotArcadeInput.lightgunPedalButton(gameId),
                pedalSize,
                RoundedCornerShape(12.dp),
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.90f, 0.72f,
                    pedalSize, pedalSize,
                ),
                fontSize = 9.sp,
            )
        } else if (driving) {
            val wheelSize = shortSide * 0.34f
            val selectSize = shortSide * 0.10f
            val pedalWidth = shortSide * 0.13f
            val pedalHeight = shortSide * 0.25f
            ArcadeWheel(
                modifier = Modifier.positionAt(
                    maxWidth, maxHeight, 0.16f, 0.70f, wheelSize, wheelSize,
                ),
            )
            ArcadeButton(
                if (battleGear) "HAZARD" else "▲",
                TeknoParrotArcadeInput.UP, selectSize, CircleShape,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.30f, 0.62f, selectSize, selectSize,
                ),
                fontSize = if (battleGear) 8.sp else 13.sp,
            )
            ArcadeButton(
                if (battleGear) "VIEW" else "▼",
                TeknoParrotArcadeInput.DOWN, selectSize, CircleShape,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.30f, 0.78f, selectSize, selectSize,
                ),
                fontSize = if (battleGear) 9.sp else 13.sp,
            )
            ArcadePedal(
                "BRAKE", TeknoParrotArcadeInput.AXIS_BRAKE,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.76f, 0.73f, pedalWidth, pedalHeight,
                ),
            )
            ArcadePedal(
                "GAS", TeknoParrotArcadeInput.AXIS_GAS,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.89f, 0.73f, pedalWidth, pedalHeight,
                ),
            )
        } else if (drum) {
            val selectSize = shortSide * 0.10f
            val drumSize = shortSide * 0.15f
            ArcadeButton(
                "▲", TeknoParrotArcadeInput.UP, selectSize, CircleShape,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.15f, 0.62f, selectSize, selectSize,
                ),
                fontSize = 13.sp,
            )
            ArcadeButton(
                "▼", TeknoParrotArcadeInput.DOWN, selectSize, CircleShape,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.15f, 0.79f, selectSize, selectSize,
                ),
                fontSize = 13.sp,
            )
            ArcadeDrumPad(
                "KA L", TeknoParrotArcadeInput.AXIS_DRUM_P1_LEFT_RIM,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.56f, 0.68f, drumSize, drumSize,
                ),
            )
            ArcadeDrumPad(
                "DON L", TeknoParrotArcadeInput.AXIS_DRUM_P1_LEFT_CENTER,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.69f, 0.68f, drumSize, drumSize,
                ),
            )
            ArcadeDrumPad(
                "DON R", TeknoParrotArcadeInput.AXIS_DRUM_P1_RIGHT_CENTER,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.82f, 0.68f, drumSize, drumSize,
                ),
            )
            ArcadeDrumPad(
                "KA R", TeknoParrotArcadeInput.AXIS_DRUM_P1_RIGHT_RIM,
                Modifier.positionAt(
                    maxWidth, maxHeight, 0.95f, 0.68f, drumSize, drumSize,
                ),
            )
        } else if (zoids) {
            val zoidsMoveSize = shortSide * 0.30f
            val zoidsJumpSize = shortSide * 0.25f
            ArcadeDPad(
                modifier = Modifier.positionAt(
                    maxWidth, maxHeight, 0.27f, 0.69f,
                    zoidsMoveSize, zoidsMoveSize,
                ),
                up = TeknoParrotArcadeInput.zoidsMoveButton(TeknoParrotArcadeInput.UP),
                down = TeknoParrotArcadeInput.zoidsMoveButton(TeknoParrotArcadeInput.DOWN),
                left = TeknoParrotArcadeInput.zoidsMoveButton(TeknoParrotArcadeInput.LEFT),
                right = TeknoParrotArcadeInput.zoidsMoveButton(TeknoParrotArcadeInput.RIGHT),
                label = "MOVE",
            )
            ArcadeDPad(
                modifier = Modifier.positionAt(
                    maxWidth, maxHeight, 0.42f, 0.66f,
                    zoidsJumpSize, zoidsJumpSize,
                ),
                up = TeknoParrotArcadeInput.zoidsJumpButton(TeknoParrotArcadeInput.UP),
                down = TeknoParrotArcadeInput.zoidsJumpButton(TeknoParrotArcadeInput.DOWN),
                left = TeknoParrotArcadeInput.zoidsJumpButton(TeknoParrotArcadeInput.LEFT),
                right = TeknoParrotArcadeInput.zoidsJumpButton(TeknoParrotArcadeInput.RIGHT),
                label = "JUMP",
            )
        } else if (gameId != "NM00037") {
            ArcadeDPad(
                modifier = Modifier.positionAt(maxWidth, maxHeight, 0.16f, 0.70f, dpadSize, dpadSize),
            )
        }

        if (!lightgun) {
            actionButtonsFor(gameId).forEach { action ->
                ArcadeButton(
                    action.label, action.button, actionSize, CircleShape,
                    Modifier.positionAt(
                        maxWidth, maxHeight,
                        action.x, action.y,
                        actionSize, actionSize,
                    ),
                )
            }
        }

        ArcadeButton(
            "COIN", TeknoParrotArcadeInput.COIN, menuWidth, RoundedCornerShape(12.dp),
            Modifier
                .positionAt(maxWidth, maxHeight, 0.42f, 0.91f, menuWidth, menuHeight)
                .size(menuWidth, menuHeight),
            fontSize = 11.sp,
        )
        ArcadeButton(
            if (drum) "ENTER" else "START",
            if (drum) TeknoParrotArcadeInput.BUTTON_1 else TeknoParrotArcadeInput.START,
            menuWidth, RoundedCornerShape(12.dp),
            Modifier
                .positionAt(maxWidth, maxHeight, 0.55f, 0.91f, menuWidth, menuHeight)
                .size(menuWidth, menuHeight),
            fontSize = 11.sp,
        )
        ArcadeButton(
            "SERVICE", TeknoParrotArcadeInput.SERVICE, utilityWidth, RoundedCornerShape(10.dp),
            Modifier
                .positionAt(maxWidth, maxHeight, 0.39f, 0.10f, utilityWidth, utilityHeight)
                .size(utilityWidth, utilityHeight),
            fontSize = 9.sp,
        )
        ArcadeButton(
            "TEST", TeknoParrotArcadeInput.TEST, utilityWidth, RoundedCornerShape(10.dp),
            Modifier
                .positionAt(maxWidth, maxHeight, 0.53f, 0.10f, utilityWidth, utilityHeight)
                .size(utilityWidth, utilityHeight),
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun LightgunAimSurface(gameId: String, modifier: Modifier) {
    val trigger = TeknoParrotArcadeInput.lightgunTriggerButton(gameId)
    Box(
        modifier = modifier.pointerInput(gameId) {
            awaitEachGesture {
                val pointerDown = awaitFirstDown(requireUnconsumed = false)
                val pointerId = pointerDown.id

                fun updateAim(position: Offset) {
                    val x = (position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val y = (position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    TeknoParrotArcadeInput.setAxis(
                        TeknoParrotArcadeInput.AXIS_STEER,
                        x * 2.0f - 1.0f,
                    )
                    TeknoParrotArcadeInput.setAxis(
                        TeknoParrotArcadeInput.AXIS_GAS,
                        y,
                    )
                }

                updateAim(pointerDown.position)
                TeknoParrotArcadeInput.set(trigger, true)
                pointerDown.consume()
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change =
                            event.changes.firstOrNull { it.id == pointerId }
                                ?: break
                        if (!change.pressed)
                            break
                        updateAim(change.position)
                        change.consume()
                    }
                } finally {
                    TeknoParrotArcadeInput.set(trigger, false)
                }
            }
        },
    )
}

object TeknoParrotArcadeInput {
    const val COIN = 0
    const val START = 1
    const val UP = 2
    const val DOWN = 3
    const val LEFT = 4
    const val RIGHT = 5
    const val BUTTON_1 = 6
    const val BUTTON_2 = 7
    const val BUTTON_3 = 8
    const val BUTTON_4 = 9
    const val BUTTON_5 = 10
    const val BUTTON_6 = 11
    const val SERVICE = 12
    const val TEST = 13
    const val BUTTON_7 = 14
    const val BUTTON_8 = 15
    const val BUTTON_9 = 16

    const val AXIS_STEER = 0
    const val AXIS_GAS = 1
    const val AXIS_BRAKE = 2
    private const val AXIS_DRUM_BASE = 3
    const val AXIS_DRUM_P1_LEFT_CENTER = AXIS_DRUM_BASE + 0
    const val AXIS_DRUM_P1_RIGHT_CENTER = AXIS_DRUM_BASE + 3
    const val AXIS_DRUM_P1_RIGHT_RIM = AXIS_DRUM_BASE + 4
    const val AXIS_DRUM_P1_LEFT_RIM = AXIS_DRUM_BASE + 5

    fun set(button: Int, pressed: Boolean) {
        NativeApp.setTeknoParrotOverlayButton(button, pressed)
    }

    fun setAxis(axis: Int, value: Float, active: Boolean = true) {
        NativeApp.setTeknoParrotOverlayAxis(axis, value, active)
    }

    private val drivingGames = setOf(
        "NM00001", "NM00005", "NM00008", "NM00010", "NM00015", "NM00039", "NM00047",
    )
    private val lightgunGames = setOf("NM00003", "NM00012", "NM00021", "NM00032")
    private val zoidsGames = setOf("NM00016", "NM00025")
    private val drumGames = setOf("NM00023", "NM00033")
    private val classicDrivingGames = setOf("NM00001", "NM00005", "NM00008", "NM00047")
    private val battleGearGames = setOf("NM00010", "NM00015")
    fun isDrivingGame(gameId: String): Boolean = gameId in drivingGames
    fun isLightgunGame(gameId: String): Boolean = gameId in lightgunGames
    fun isBattleGearGame(gameId: String): Boolean = gameId in battleGearGames
    fun isZoidsGame(gameId: String): Boolean = gameId in zoidsGames
    fun isDrumGame(gameId: String): Boolean = gameId in drumGames
    fun lightgunTriggerButton(gameId: String): Int = when (gameId) {
        "NM00021", "NM00032" -> LEFT
        else -> BUTTON_2
    }
    fun lightgunPedalButton(gameId: String): Int = when (gameId) {
        "NM00012" -> BUTTON_6
        else -> BUTTON_3
    }

    fun gamepadDrumAxis(gameId: String, keyCode: Int): Int? {
        if (!isDrumGame(gameId))
            return null
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_X -> AXIS_DRUM_P1_LEFT_CENTER
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1 -> AXIS_DRUM_P1_LEFT_RIM
            KeyEvent.KEYCODE_BUTTON_A -> AXIS_DRUM_P1_RIGHT_CENTER
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_R1 -> AXIS_DRUM_P1_RIGHT_RIM
            else -> null
        }
    }

    // Zoids uses two four-way lever groups wired across otherwise unrelated
    // JVS switches. Preserve the physical cabinet directions instead of
    // presenting a conventional D-pad which would send the wrong switches.
    fun zoidsMoveButton(direction: Int): Int = when (direction) {
        UP -> DOWN
        DOWN -> LEFT
        LEFT -> RIGHT
        RIGHT -> BUTTON_1
        else -> direction
    }

    fun zoidsJumpButton(direction: Int): Int = when (direction) {
        UP -> BUTTON_2
        DOWN -> BUTTON_3
        LEFT -> BUTTON_4
        RIGHT -> BUTTON_5
        else -> direction
    }

    private val tekkenGames = setOf("NM00004", "NM00019", "NM00026", "NM00027", "NM00011")
    private val gundamGames = setOf(
        "NM00013", "NM00017", "NM00024", "NM00034",
        "NM00035", "NM00043", "NM00052",
    )
    private val soulCaliburGames = setOf(
        "NM00007", "NM00031", "NM00048", "NM00029", "NM00040", "NM00042",
    )
    private val sixButtonGames = setOf("NM00009", "NM00018")

    /**
     * Cabinet mapping for Android/Xbox-style physical controllers. It mirrors
     * ACJV's official-PS2-port rows rather than assuming every title uses the
     * same switch numbering.
     */
    fun gamepadButton(gameId: String, keyCode: Int): Int? {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_START -> if (isDrumGame(gameId)) BUTTON_1 else START
            KeyEvent.KEYCODE_BUTTON_SELECT -> COIN
            KeyEvent.KEYCODE_DPAD_UP ->
                if (isZoidsGame(gameId)) zoidsMoveButton(UP) else UP
            KeyEvent.KEYCODE_DPAD_DOWN ->
                if (isZoidsGame(gameId)) zoidsMoveButton(DOWN) else DOWN
            KeyEvent.KEYCODE_DPAD_LEFT ->
                if (isZoidsGame(gameId)) zoidsMoveButton(LEFT) else LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT ->
                if (isZoidsGame(gameId)) zoidsMoveButton(RIGHT) else RIGHT
            KeyEvent.KEYCODE_BUTTON_X -> when {
                gameId == "NM00037" -> UP
                isZoidsGame(gameId) -> BUTTON_6
                gameId == "NM00030" -> BUTTON_1
                else -> BUTTON_1
            }
            KeyEvent.KEYCODE_BUTTON_Y -> when (gameId) {
                "NM00037" -> DOWN
                "NM00016", "NM00025" -> BUTTON_9
                "NM00030" -> BUTTON_2
                "NM00047" -> BUTTON_9
                "NM00001", "NM00005", "NM00008" -> BUTTON_2
                "NM00010", "NM00015" -> DOWN
                "NM00002" -> BUTTON_4
                else -> BUTTON_2
            }
            KeyEvent.KEYCODE_BUTTON_A -> when {
                gameId == "NM00037" -> LEFT
                isZoidsGame(gameId) -> BUTTON_8
                gameId == "NM00003" -> BUTTON_3
                gameId in tekkenGames -> BUTTON_4
                gameId in gundamGames -> BUTTON_3
                gameId in soulCaliburGames -> BUTTON_4
                gameId == "NM00030" -> BUTTON_3
                gameId == "NM00002" -> BUTTON_2
                gameId in sixButtonGames -> BUTTON_4
                else -> BUTTON_1
            }
            KeyEvent.KEYCODE_BUTTON_B -> when {
                gameId == "NM00037" -> RIGHT
                isZoidsGame(gameId) -> BUTTON_7
                gameId in tekkenGames -> BUTTON_5
                gameId in gundamGames -> BUTTON_4
                gameId in soulCaliburGames -> BUTTON_3
                gameId == "NM00030" -> BUTTON_4
                gameId == "NM00047" -> BUTTON_9
                gameId in battleGearGames -> DOWN
                gameId == "NM00002" -> BUTTON_3
                gameId in sixButtonGames -> BUTTON_5
                else -> BUTTON_2
            }
            KeyEvent.KEYCODE_BUTTON_L1 -> when {
                gameId in sixButtonGames -> BUTTON_3
                gameId in zoidsGames -> BUTTON_4
                gameId in classicDrivingGames -> BUTTON_4
                gameId in battleGearGames -> LEFT
                else -> BUTTON_5
            }
            KeyEvent.KEYCODE_BUTTON_R1 -> when {
                gameId in zoidsGames -> BUTTON_5
                gameId in classicDrivingGames -> BUTTON_3
                gameId in battleGearGames -> RIGHT
                else -> BUTTON_6
            }
            else -> null
        }
    }
}

private data class ArcadeActionButton(
    val label: String,
    val button: Int,
    val x: Float,
    val y: Float,
)

private fun actionButtonsFor(gameId: String): List<ArcadeActionButton> {
    fun two(labels: List<String>, buttons: List<Int>) = listOf(
        ArcadeActionButton(labels[0], buttons[0], 0.80f, 0.68f),
        ArcadeActionButton(labels[1], buttons[1], 0.91f, 0.54f),
    )
    fun four(labels: List<String>, buttons: List<Int>) = listOf(
        ArcadeActionButton(labels[0], buttons[0], 0.79f, 0.71f),
        ArcadeActionButton(labels[1], buttons[1], 0.90f, 0.61f),
        ArcadeActionButton(labels[2], buttons[2], 0.72f, 0.51f),
        ArcadeActionButton(labels[3], buttons[3], 0.83f, 0.41f),
    )

    return when (gameId) {
        "NM00023", "NM00033" -> emptyList()
        "NM00037" -> four(
            listOf("1", "2", "3", "4"),
            listOf(
                TeknoParrotArcadeInput.UP,
                TeknoParrotArcadeInput.DOWN,
                TeknoParrotArcadeInput.LEFT,
                TeknoParrotArcadeInput.RIGHT,
            ),
        )
        "NM00011" -> four(
            listOf("LP", "RP", "LK", "RK"),
            listOf(
                TeknoParrotArcadeInput.BUTTON_1,
                TeknoParrotArcadeInput.BUTTON_2,
                TeknoParrotArcadeInput.BUTTON_4,
                TeknoParrotArcadeInput.BUTTON_5,
            ),
        )
        "NM00027" -> four(
            listOf("LIGHT", "HEAVY", "GUARD", "JUMP"),
            listOf(
                TeknoParrotArcadeInput.BUTTON_1,
                TeknoParrotArcadeInput.BUTTON_2,
                TeknoParrotArcadeInput.BUTTON_4,
                TeknoParrotArcadeInput.BUTTON_5,
            ),
        )
        "NM00029", "NM00040" -> four(
            listOf("PUNCH", "GRIP", "UPPER", "STANCE"),
            listOf(
                TeknoParrotArcadeInput.BUTTON_1,
                TeknoParrotArcadeInput.BUTTON_2,
                TeknoParrotArcadeInput.BUTTON_3,
                TeknoParrotArcadeInput.BUTTON_4,
            ),
        )
        "NM00030" -> four(
            listOf("A", "B", "C", "D"),
            listOf(
                TeknoParrotArcadeInput.BUTTON_1,
                TeknoParrotArcadeInput.BUTTON_2,
                TeknoParrotArcadeInput.BUTTON_3,
                TeknoParrotArcadeInput.BUTTON_4,
            ),
        )
        "NM00003" -> listOf(
            ArcadeActionButton("1", TeknoParrotArcadeInput.BUTTON_1, 0.79f, 0.71f),
            ArcadeActionButton("2", TeknoParrotArcadeInput.BUTTON_2, 0.90f, 0.61f),
            ArcadeActionButton("3", TeknoParrotArcadeInput.BUTTON_3, 0.72f, 0.51f),
        )
        "NM00035" -> listOf(
            ArcadeActionButton("1", TeknoParrotArcadeInput.BUTTON_1, 0.79f, 0.71f),
            ArcadeActionButton("2", TeknoParrotArcadeInput.BUTTON_2, 0.90f, 0.61f),
            ArcadeActionButton("3", TeknoParrotArcadeInput.BUTTON_3, 0.72f, 0.51f),
        )
        "NM00004", "NM00019", "NM00026" -> four(
            listOf("1", "2", "4", "5"),
            listOf(
                TeknoParrotArcadeInput.BUTTON_1,
                TeknoParrotArcadeInput.BUTTON_2,
                TeknoParrotArcadeInput.BUTTON_4,
                TeknoParrotArcadeInput.BUTTON_5,
            ),
        )
        "NM00009", "NM00018" -> listOf(
            ArcadeActionButton("1", TeknoParrotArcadeInput.BUTTON_1, 0.79f, 0.71f),
            ArcadeActionButton("2", TeknoParrotArcadeInput.BUTTON_2, 0.90f, 0.61f),
            ArcadeActionButton("3", TeknoParrotArcadeInput.BUTTON_3, 0.72f, 0.51f),
            ArcadeActionButton("4", TeknoParrotArcadeInput.BUTTON_4, 0.83f, 0.41f),
            ArcadeActionButton("5", TeknoParrotArcadeInput.BUTTON_5, 0.67f, 0.32f),
            ArcadeActionButton("6", TeknoParrotArcadeInput.BUTTON_6, 0.77f, 0.25f),
        )
        "NM00047" -> listOf(
            ArcadeActionButton("ENTER", TeknoParrotArcadeInput.BUTTON_1, 0.69f, 0.38f),
            ArcadeActionButton("VIEW", TeknoParrotArcadeInput.BUTTON_9, 0.80f, 0.31f),
            ArcadeActionButton("GEAR+", TeknoParrotArcadeInput.BUTTON_3, 0.91f, 0.38f),
            ArcadeActionButton("GEAR-", TeknoParrotArcadeInput.BUTTON_4, 0.79f, 0.48f),
        )
        "NM00001", "NM00005", "NM00008" -> listOf(
            ArcadeActionButton("ENTER", TeknoParrotArcadeInput.BUTTON_1, 0.69f, 0.38f),
            ArcadeActionButton("VIEW", TeknoParrotArcadeInput.BUTTON_2, 0.80f, 0.31f),
            ArcadeActionButton("GEAR+", TeknoParrotArcadeInput.BUTTON_3, 0.91f, 0.38f),
            ArcadeActionButton("GEAR-", TeknoParrotArcadeInput.BUTTON_4, 0.79f, 0.48f),
        )
        "NM00010", "NM00015" -> listOf(
            ArcadeActionButton("GEAR-", TeknoParrotArcadeInput.BUTTON_1, 0.69f, 0.38f),
            ArcadeActionButton("GEAR+", TeknoParrotArcadeInput.RIGHT, 0.80f, 0.31f),
            ArcadeActionButton("SIDE BRK", TeknoParrotArcadeInput.LEFT, 0.91f, 0.38f),
        )
        "NM00016", "NM00025" -> listOf(
            ArcadeActionButton("CANNON", TeknoParrotArcadeInput.BUTTON_6, 0.72f, 0.71f),
            ArcadeActionButton("C-LOAD", TeknoParrotArcadeInput.BUTTON_9, 0.77f, 0.61f),
            ArcadeActionButton("ROCKET", TeknoParrotArcadeInput.BUTTON_8, 0.65f, 0.51f),
            ArcadeActionButton("R-LOAD", TeknoParrotArcadeInput.BUTTON_7, 0.74f, 0.41f),
        )
        "NM00002", "NM00007", "NM00013", "NM00017", "NM00024",
        "NM00031", "NM00034",
        "NM00042", "NM00043", "NM00048", "NM00052" -> four(
            listOf("1", "2", "3", "4"),
            listOf(
                TeknoParrotArcadeInput.BUTTON_1,
                TeknoParrotArcadeInput.BUTTON_2,
                TeknoParrotArcadeInput.BUTTON_3,
                TeknoParrotArcadeInput.BUTTON_4,
            ),
        )
        else -> two(
            listOf("1", "2"),
            listOf(TeknoParrotArcadeInput.BUTTON_1, TeknoParrotArcadeInput.BUTTON_2),
        )
    }
}

private val ArcadeLine = Color.White.copy(alpha = 0.72f)
private val ArcadeFill = Color.Black.copy(alpha = 0.24f)
private val ArcadePressed = Color.White.copy(alpha = 0.32f)

@Composable
private fun ArcadeButton(
    label: String,
    button: Int,
    size: Dp,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
) {
    var pressed by remember(button) { mutableStateOf(false) }
    DisposableEffect(button) {
        onDispose { TeknoParrotArcadeInput.set(button, false) }
    }

    Box(
        modifier = modifier
            .then(if (modifier == Modifier) Modifier.size(size) else Modifier)
            .background(if (pressed) ArcadePressed else ArcadeFill, shape)
            .border(2.dp, ArcadeLine, shape)
            .pointerInput(button) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        TeknoParrotArcadeInput.set(button, true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            TeknoParrotArcadeInput.set(button, false)
                            pressed = false
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ArcadeDPad(
    modifier: Modifier,
    up: Int = TeknoParrotArcadeInput.UP,
    down: Int = TeknoParrotArcadeInput.DOWN,
    left: Int = TeknoParrotArcadeInput.LEFT,
    right: Int = TeknoParrotArcadeInput.RIGHT,
    label: String? = null,
) {
    var held by remember { mutableStateOf(emptySet<Int>()) }

    fun updateHeld(next: Set<Int>) {
        (held - next).forEach { TeknoParrotArcadeInput.set(it, false) }
        (next - held).forEach { TeknoParrotArcadeInput.set(it, true) }
        held = next
    }

    DisposableEffect(Unit) {
        onDispose {
            held.forEach { TeknoParrotArcadeInput.set(it, false) }
            held = emptySet()
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val pointerDown = awaitFirstDown(requireUnconsumed = false)
                val pointerId = pointerDown.id

                fun directions(position: Offset): Set<Int> {
                    val dx = (position.x - size.width / 2f) / (size.width / 2f)
                    val dy = (position.y - size.height / 2f) / (size.height / 2f)
                    val deadZone = 0.20f
                    if (abs(dx) < deadZone && abs(dy) < deadZone)
                        return emptySet()
                    return buildSet {
                        if (dx <= -deadZone) add(left)
                        if (dx >= deadZone) add(right)
                        if (dy <= -deadZone) add(up)
                        if (dy >= deadZone) add(down)
                    }
                }

                updateHeld(directions(pointerDown.position))
                pointerDown.consume()
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed)
                            break
                        updateHeld(directions(change.position))
                        change.consume()
                    }
                } finally {
                    updateHeld(emptySet())
                }
            }
        },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = min(size.width, size.height) * 0.025f
            val bar = size.width * 0.34f
            val inset = size.width * 0.08f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(ArcadeFill, radius = size.minDimension / 2f, center = center)
            drawCircle(
                ArcadeLine,
                radius = size.minDimension / 2f - stroke / 2f,
                center = center,
                style = Stroke(stroke),
            )
            drawRoundRect(
                if (held.any {
                    it == left || it == right
                }) ArcadePressed else ArcadeFill,
                topLeft = Offset(inset, center.y - bar / 2f),
                size = Size(size.width - inset * 2f, bar),
            )
            drawRoundRect(
                if (held.any {
                    it == up || it == down
                }) ArcadePressed else ArcadeFill,
                topLeft = Offset(center.x - bar / 2f, inset),
                size = Size(bar, size.height - inset * 2f),
            )
        }
        Text("▲", Modifier.align(Alignment.TopCenter), color = ArcadeLine, fontSize = 18.sp)
        Text("▼", Modifier.align(Alignment.BottomCenter), color = ArcadeLine, fontSize = 18.sp)
        Text("◀", Modifier.align(Alignment.CenterStart), color = ArcadeLine, fontSize = 18.sp)
        Text("▶", Modifier.align(Alignment.CenterEnd), color = ArcadeLine, fontSize = 18.sp)
        if (label != null) {
            Text(
                label,
                Modifier.align(Alignment.Center),
                color = ArcadeLine,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArcadeWheel(modifier: Modifier) {
    var steer by remember { mutableStateOf(0f) }

    fun update(value: Float) {
        steer = value.coerceIn(-1f, 1f)
        TeknoParrotArcadeInput.setAxis(
            TeknoParrotArcadeInput.AXIS_STEER, steer, active = true,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            TeknoParrotArcadeInput.setAxis(
                TeknoParrotArcadeInput.AXIS_STEER, 0f, active = false,
            )
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                fun value(position: Offset): Float =
                    ((position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)

                update(value(down.position))
                down.consume()
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed)
                            break
                        update(value(change.position))
                        change.consume()
                    }
                } finally {
                    // An arcade wheel self-centers when released. Keep the local
                    // center for this frame, then release ownership to TPUI.
                    update(0f)
                    TeknoParrotArcadeInput.setAxis(
                        TeknoParrotArcadeInput.AXIS_STEER, 0f, active = false,
                    )
                    steer = 0f
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension * 0.08f
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - stroke
            drawCircle(ArcadeFill, radius = radius, center = center)
            drawCircle(ArcadeLine, radius = radius, center = center, style = Stroke(stroke))
            // Draw a recognizable three-spoke steering wheel and rotate it with
            // the live steering position. The previous single diagonal line did
            // not resemble a wheel at all.
            rotate(degrees = steer * 120f, pivot = center) {
                val spokeWidth = stroke * 0.52f
                drawLine(
                    ArcadeLine,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.82f),
                    strokeWidth = spokeWidth,
                )
                drawLine(
                    ArcadeLine,
                    start = center,
                    end = Offset(center.x - radius * 0.71f, center.y + radius * 0.41f),
                    strokeWidth = spokeWidth,
                )
                drawLine(
                    ArcadeLine,
                    start = center,
                    end = Offset(center.x + radius * 0.71f, center.y + radius * 0.41f),
                    strokeWidth = spokeWidth,
                )
            }
            drawCircle(
                if (abs(steer) > 0.02f) ArcadePressed else ArcadeLine,
                radius = stroke * 0.72f,
                center = center,
            )
        }
        Text(
            "STEER",
            Modifier.align(Alignment.BottomCenter),
            color = ArcadeLine,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ArcadePedal(label: String, axis: Int, modifier: Modifier) {
    var pressed by remember(axis) { mutableStateOf(false) }
    DisposableEffect(axis) {
        onDispose { TeknoParrotArcadeInput.setAxis(axis, 0f, active = false) }
    }

    Box(
        modifier = modifier
            .background(
                if (pressed) ArcadePressed else ArcadeFill,
                RoundedCornerShape(14.dp),
            )
            .border(2.dp, ArcadeLine, RoundedCornerShape(14.dp))
            .pointerInput(axis) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        TeknoParrotArcadeInput.setAxis(axis, 1f, active = true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            TeknoParrotArcadeInput.setAxis(axis, 0f, active = false)
                            pressed = false
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ArcadeDrumPad(label: String, axis: Int, modifier: Modifier) {
    var pressed by remember(axis) { mutableStateOf(false) }
    DisposableEffect(axis) {
        onDispose { TeknoParrotArcadeInput.setAxis(axis, 0f, active = false) }
    }

    Box(
        modifier = modifier
            .background(
                if (pressed) ArcadePressed else ArcadeFill,
                CircleShape,
            )
            .border(3.dp, ArcadeLine, CircleShape)
            .pointerInput(axis) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        TeknoParrotArcadeInput.setAxis(axis, 1f, active = true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            TeknoParrotArcadeInput.setAxis(axis, 0f, active = false)
                            pressed = false
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Modifier.positionAt(
    width: Dp,
    height: Dp,
    xFraction: Float,
    yFraction: Float,
    itemWidth: Dp,
    itemHeight: Dp,
): Modifier = this
    .offset(
        x = width * xFraction - itemWidth / 2,
        y = height * yFraction - itemHeight / 2,
    )
    .size(itemWidth, itemHeight)
