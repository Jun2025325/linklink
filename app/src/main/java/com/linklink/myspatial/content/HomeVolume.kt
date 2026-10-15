package com.linklink.myspatial.content

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import com.linklink.myspatial.R
import com.linklink.myspatial.game.Cell
import com.linklink.myspatial.game.LianliankanGame
import com.linklink.myspatial.game.MoveResult
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MaterialCullingMode
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.content.panelSize
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.foundation.gesture.detectSpatialPointerEvent
import com.pico.spatial.ui.foundation.gesture.detectSpatialTapGesture
import com.pico.spatial.ui.foundation.gesture.detectSpatialTransformGesture
import com.pico.spatial.ui.foundation.gesture.data.InteractionKind
import com.pico.spatial.ui.geometry.Offset3D
import com.pico.spatial.ui.foundation.haptic.controllerHapticFeedback
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.hover.disableSpatialHoverEffect
import com.pico.spatial.ui.platform.HandController
import com.pico.spatial.ui.platform.HandControllerHapticType
import com.pico.spatial.ui.platform.LocalHandControllerHaptic
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.pico.spatial.tracking.hand.HandJoint
import com.pico.spatial.tracking.hand.HandTrackingData
import com.pico.spatial.tracking.hand.HandTrackingProvider
import java.util.Locale
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

private const val GRID_ROWS = 4
private const val GRID_COLS = 4
private const val GRID_LAYERS = 4
private const val FEEL_CARD_DRAG_TRANSFORM_SLOP = 18f
private const val FEEL_HOLD_BOARD_ROTATION_MS = 320L
private const val SELECTED_SCALE = 1.24f
private const val INITIAL_BOARD_YAW = 28f
private const val CARD_TEXTURE_SIZE = 128
private const val YAW_DEGREES_PER_PIXEL = 0.18f
private const val PITCH_DEGREES_PER_PIXEL = 0.14f
private const val MAX_BOARD_PITCH = 90f
private const val ROTATION_FIT_SCALE = 0.76f
private const val STATUS_PANEL_Z = 0.31f
private val IS_PICO_EMULATOR = Build.HARDWARE.equals("ranchu", ignoreCase = true)

// The emulator's default viewpoint looks toward +Z, while PICO hardware places
// the player on the opposite side of the Stage origin. Keep the authored
// emulator layout, but mirror the hardware depth so a new game starts in front
// of the player instead of behind them.
private val STAGE_CONTENT_Z = if (IS_PICO_EMULATOR) 1.0f else -1.0f
private const val STAGE_CONTENT_Y = 1.45f
private const val TOOL_RAIL_X = 0.400f
private const val TOOL_RAIL_Y = 1.48f
private val TOOL_RAIL_Z = if (IS_PICO_EMULATOR) 1.10f else -0.78f
private const val TOOL_PANEL_WIDTH = 0.140f
private const val TOOL_PANEL_HEIGHT = 0.230f
private const val BOARD_MOVE_GAIN = 2.2f
private const val PANEL_DRAG_SLOP = 4f
private const val PANEL_TWO_HAND_MOVE_GAIN = 0.72f
private const val PANEL_TWO_HAND_YAW_GAIN = 0.88f
private const val PANEL_TWO_HAND_PITCH_GAIN = 0.72f
private const val MAX_PANEL_PITCH = 45f
private val TOOL_RAIL_DEFAULT_POSITION = Vector3(
    x = TOOL_RAIL_X,
    y = TOOL_RAIL_Y,
    z = TOOL_RAIL_Z,
)
private const val SHUFFLE_DURATION_MS = 1150
private const val SHUFFLE_STAGGER_MS = 16
private const val CONTROL_BOB_DURATION_MS = 2400
private const val FEEL_GRAB_SCALE = 1.22f
private const val FEEL_SPRING = 135f
private const val FEEL_DAMPING = 15f
private const val FEEL_MAX_SPEED = 2.6f
private const val FEEL_DRAG_LIMIT = 0.38f
private const val FEEL_DROP_RADIUS_FACTOR = 0.90f
private const val FEEL_MAGNET_RADIUS_FACTOR = 2.0f
private const val FEEL_MAGNET_STRENGTH = 0.22f
private const val FEEL_TARGET_HIGHLIGHT_FACTOR = 1.65f
private const val FEEL_CARD_COLLISION_FACTOR = 1.15f
private const val FEEL_CARD_MAGNET_FACTOR = 2.10f
private const val FEEL_CARD_MAGNET_STRENGTH = 0.20f
private const val FEEL_HAND_TRACK_FRAME_MS = 16L
private const val FEEL_FLICK_SPEED_METERS_PER_SECOND = 1.35f
private const val FEEL_FLICK_SMOOTHED_SPEED = 1.0f
private const val FEEL_FLICK_FAST_FRAMES = 3
private const val FEEL_FLICK_SELECT_COOLDOWN_MS = 450L
private const val TWO_HAND_MEET_DURATION_MS = 240
private const val TWO_HAND_REJECT_DURATION_MS = 360
private const val CONTROL_WIDTH = 0.092f
private const val CONTROL_HEIGHT = 0.076f
private const val CONTROL_DEPTH = 0.022f
private const val CONTROL_CORNER_RADIUS = 0.026f
private const val CONTROL_HOVER_SCALE = 1.10f
private const val CONTROL_PRESS_SCALE = 0.90f
private const val CONTROL_HOVER_Z = 0.015f
private const val CONTROL_PRESS_Z = -0.008f
private const val STATUS_PANEL_DEPTH = 0.022f
private const val STATUS_PANEL_CORNER_RADIUS = 0.026f
private const val SETUP_PANEL_WIDTH = 0.50f
private const val SETUP_PANEL_HEIGHT = 0.415f
private const val SETUP_PANEL_DEPTH = 0.030f
private const val SETUP_PANEL_CORNER_RADIUS = 0.042f
// A true omnidirectional sphere avoids cube-face clipping/compositing seams:
// every yaw/pitch direction has one continuous interior surface.
private const val COSMOS_BACKGROUND_RADIUS = 14f
private const val CONTROL_ROW_Y = -0.275f
private const val CONTROL_ROW_Z = 0.34f
private const val MIN_PLAYABLE_CARD_SIZE = 0.058f
private const val INTERACTION_TAG = "LinklinInteraction"
private const val CELEBRATION_PARTICLE_SIZE = 0.013f
private const val CELEBRATION_PARTICLE_COUNT = 220
private const val CELEBRATION_DURATION_MS = 7600f
private const val CELEBRATION_FRAME_MS = 33f
private const val EFFECT_PARTICLE_SIZE = 0.010f
private const val EFFECT_PARTICLE_COUNT = 210
private const val EFFECT_FRAME_MS = 33f
private const val ENERGY_LINK_SEGMENTS = 18

private val SETUP_PANEL_DEFAULT_POSITION = Vector3(
    x = 0.00f,
    y = 0.82f,
    z = -0.68f,
)

private val DIRECT_SETUP_PANEL_DEFAULT_POSITION = Vector3(
    x = 0.00f,
    y = 1.90f,
    z = 0.82f,
)

private val CELEBRATION_COLORS = listOf(
    Color4(1.00f, 0.42f, 0.46f, 1f),
    Color4(1.00f, 0.80f, 0.29f, 1f),
    Color4(0.55f, 0.86f, 1.00f, 1f),
    Color4(0.61f, 0.93f, 0.74f, 1f),
    Color4(0.83f, 0.68f, 1.00f, 1f),
    Color4(1.00f, 0.65f, 0.83f, 1f),
)

/**
 * PICO reports a near-hand pinch as DirectPinch, but the same physical action
 * can enter the Compose ray/gaze path as GazePinch or RayBasedPinch. Feel mode
 * deliberately accepts all pinch sources while rejecting plain Pointer input;
 * casual selection remains GazePinch-only.
 */
private enum class FeelHand {
    LEFT,
    RIGHT,
}

private fun FeelHand.opposite(): FeelHand =
    if (this == FeelHand.LEFT) FeelHand.RIGHT else FeelHand.LEFT

private fun isFeelSelectionTap(kind: InteractionKind): Boolean =
    kind == InteractionKind.DirectPinch ||
        kind == InteractionKind.GazePinch ||
        kind == InteractionKind.RayBasedPinch ||
        kind == InteractionKind.Poke

enum class AppLanguage(
    val locale: Locale,
) {
    CHINESE(Locale.SIMPLIFIED_CHINESE),
    ENGLISH(Locale.ENGLISH),
}

private fun handAnchor(
    data: HandTrackingData,
    hand: FeelHand,
): Vector3? {
    val pose = if (hand == FeelHand.LEFT) data.left else data.right
    val pinch = if (hand == FeelHand.LEFT) {
        data.leftPinch
    } else {
        data.rightPinch
    }
    return pinch?.takeIf { it.isReady }?.pinchPose?.position
        ?: pose?.get(HandJoint.Index.PALM)?.position
}

private fun distance(a: Vector3, b: Vector3): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun clampedSetupPanelPosition(position: Vector3): Vector3 = Vector3(
    x = position.x.coerceIn(-0.78f, 0.78f),
    y = position.y.coerceIn(0.35f, 2.05f),
    z = position.z.coerceIn(-1.50f, -0.45f),
)

/**
 * Imperative placement for the large setup AttachmentPanel. Position is deliberately
 * not Compose state: dragging updates the bound Entity directly, exactly like the
 * stable tool-rail path, without recomposing the whole setup sheet every frame.
 */
private class SetupPanelPlacement {
    var position: Vector3 by mutableStateOf(SETUP_PANEL_DEFAULT_POSITION)
        private set
    var yaw: Float by mutableStateOf(0f)
        private set
    var pitch: Float by mutableStateOf(0f)
        private set

    private var entity: Entity? = null

    fun bind(entity: Entity) {
        this.entity = entity
        apply()
    }

    fun clearIfCurrent(entity: Entity?) {
        if (this.entity === entity) this.entity = null
    }

    fun move(delta: Vector3) {
        // Pinch depth is intentionally excluded: the large sheet otherwise appears
        // to flash forward/backward from natural hand-depth jitter.
        position = clampedSetupPanelPosition(
            Vector3(
                x = position.x + delta.x,
                y = position.y + delta.y,
                z = position.z,
            ),
        )
        apply()
    }

    fun rotate(
        yawDeltaDegrees: Float,
        pitchDeltaDegrees: Float,
    ) {
        yaw = (yaw + yawDeltaDegrees) % 360f
        pitch = (pitch + pitchDeltaDegrees)
            .coerceIn(-MAX_PANEL_PITCH, MAX_PANEL_PITCH)
        apply()
    }

    fun bringToFront() {
        position = clampedSetupPanelPosition(
            Vector3(
                x = position.x,
                y = position.y,
                z = -0.56f,
            ),
        )
        apply()
    }

    fun apply() {
        val current = entity ?: return
        if (!current.valid) {
            entity = null
            return
        }
        current.components[TransformComponent::class.java]?.apply {
            setPosition(clampedSetupPanelPosition(position))
            setEulerAngles(EulerAngles(pitch = pitch, yaw = yaw))
        }
    }
}

private fun clampedToolRailPosition(position: Vector3): Vector3 = Vector3(
    x = position.x.coerceIn(-0.78f, 0.78f),
    y = position.y.coerceIn(0.35f, 2.05f),
    z = if (IS_PICO_EMULATOR) {
        position.z.coerceIn(0.45f, 1.50f)
    } else {
        position.z.coerceIn(-1.50f, -0.45f)
    },
)

/**
 * A direct two-mode UI-panel gesture. One hand moves the panel. Two hands can
 * simultaneously move it and rotate it from the line between the two pointers.
 */
@Composable
private fun Modifier.directPanelGesture(
    logName: String,
    onMove: (Vector3) -> Unit,
    onRotate: (yawDeltaDegrees: Float, pitchDeltaDegrees: Float) -> Unit,
    onGestureEnd: (
        moveDelta: Vector3,
        yawDeltaDegrees: Float,
        pitchDeltaDegrees: Float,
    ) -> Unit = { _, _, _ -> },
): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current
    val physicalLengthConverter = LocalPhysicalLengthConverter.current
    val metersPerPixel =
        physicalLengthConverter.dpToLength(1.dp, LengthUnit.Meters) / density.density
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnRotate by rememberUpdatedState(onRotate)
    val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)

    return pointerInput(logName) {
        val lastPositions = LinkedHashMap<PointerId, Offset3D>()
        val lastDevicePositions = LinkedHashMap<PointerId, Offset3D>()
        var singleHandDragging = false
        var twoHandsDragging = false
        var accumulatedMove = Vector3(0f, 0f, 0f)
        var accumulatedYaw = 0f
        var accumulatedPitch = 0f

        fun resetGesture() {
            lastPositions.clear()
            lastDevicePositions.clear()
            singleHandDragging = false
            twoHandsDragging = false
            accumulatedMove = Vector3(0f, 0f, 0f)
            accumulatedYaw = 0f
            accumulatedPitch = 0f
        }

        fun center(values: Collection<Offset3D>): Offset3D {
            if (values.isEmpty()) return Offset3D.Zero
            var x = 0f
            var y = 0f
            var z = 0f
            values.forEach {
                x += it.x
                y += it.y
                z += it.z
            }
            val count = values.size.toFloat()
            return Offset3D(x / count, y / count, z / count)
        }

        fun yawDegrees(): Float {
            val points = lastDevicePositions.values.toList()
            if (points.size < 2) return 0f
            val first = points[0]
            val second = points[1]
            return Math.toDegrees(
                atan2(
                    second.z - first.z,
                    second.x - first.x,
                ).toDouble(),
            ).toFloat()
        }

        fun pitchDegrees(): Float {
            val points = lastDevicePositions.values.toList()
            if (points.size < 2) return 0f
            val first = points[0]
            val second = points[1]
            val horizontal = sqrt(
                square(second.x - first.x) + square(second.z - first.z),
            )
            return Math.toDegrees(
                atan2(second.y - first.y, horizontal).toDouble(),
            ).toFloat()
        }

        fun angleDelta(current: Float, previous: Float): Float {
            var delta = current - previous
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            return delta
        }

        detectSpatialPointerEvent(context = context) { events ->
            var consume = false
            events.forEach { pointer ->
                if (pointer.isDownEvent()) {
                    if (lastDevicePositions.size >= 2) resetGesture()
                    if (singleHandDragging) {
                        resetGesture()
                    }
                    lastPositions[pointer.pointerId] = pointer.position3D
                    lastDevicePositions[pointer.pointerId] =
                        pointer.inputDevicePose.rawPosition
                    return@forEach
                }

                if (pointer.isUpEvent()) {
                    if (singleHandDragging || twoHandsDragging) {
                        Log.i(
                            INTERACTION_TAG,
                            "${logName}DragEnd " +
                                "action=${if (twoHandsDragging) "twoHand" else "move"} source=ui",
                        )
                        currentOnGestureEnd(
                            accumulatedMove,
                            accumulatedYaw,
                            accumulatedPitch,
                        )
                    }
                    resetGesture()
                    return@forEach
                }

                val previousPosition = lastPositions[pointer.pointerId]
                    ?: pointer.position3D.also {
                        lastPositions[pointer.pointerId] = it
                        lastDevicePositions[pointer.pointerId] =
                            pointer.inputDevicePose.rawPosition
                    }

                if (lastPositions.size >= 2) {
                    val previousCenter = center(lastPositions.values)
                    val previousYaw = yawDegrees()
                    val previousPitch = pitchDegrees()
                    lastPositions[pointer.pointerId] = pointer.position3D
                    lastDevicePositions[pointer.pointerId] =
                        pointer.inputDevicePose.rawPosition
                    val currentCenter = center(lastPositions.values)
                    val centerDelta = Offset3D(
                        currentCenter.x - previousCenter.x,
                        currentCenter.y - previousCenter.y,
                        currentCenter.z - previousCenter.z,
                    )
                    val yawDelta = angleDelta(yawDegrees(), previousYaw)
                    val pitchDelta = angleDelta(pitchDegrees(), previousPitch)

                    if (!twoHandsDragging) {
                        val moved = sqrt(
                            square(centerDelta.x) + square(centerDelta.y) +
                                square(centerDelta.z),
                        )
                        if (moved >= PANEL_DRAG_SLOP || abs(yawDelta) > 0.8f) {
                            twoHandsDragging = true
                            singleHandDragging = false
                            Log.i(
                                INTERACTION_TAG,
                                "${logName}DragStart action=twoHand source=ui",
                            )
                        }
                    }

                    if (twoHandsDragging) {
                        accumulatedMove = Vector3(
                            x = accumulatedMove.x + centerDelta.x *
                                metersPerPixel * PANEL_TWO_HAND_MOVE_GAIN,
                            y = accumulatedMove.y - centerDelta.y *
                                metersPerPixel * PANEL_TWO_HAND_MOVE_GAIN,
                            z = accumulatedMove.z + centerDelta.z *
                                metersPerPixel * PANEL_TWO_HAND_MOVE_GAIN,
                        )
                        accumulatedYaw += yawDelta * PANEL_TWO_HAND_YAW_GAIN
                        accumulatedPitch += pitchDelta * PANEL_TWO_HAND_PITCH_GAIN
                        currentOnMove(
                            Vector3(
                                x = centerDelta.x * metersPerPixel *
                                    PANEL_TWO_HAND_MOVE_GAIN,
                                y = -centerDelta.y * metersPerPixel *
                                    PANEL_TWO_HAND_MOVE_GAIN,
                                z = centerDelta.z * metersPerPixel *
                                    PANEL_TWO_HAND_MOVE_GAIN,
                            ),
                        )
                        currentOnRotate(
                            yawDelta * PANEL_TWO_HAND_YAW_GAIN,
                            pitchDelta * PANEL_TWO_HAND_PITCH_GAIN,
                        )
                        consume = true
                    }
                } else {
                    val delta = Offset3D(
                        pointer.position3D.x - previousPosition.x,
                        pointer.position3D.y - previousPosition.y,
                        pointer.position3D.z - previousPosition.z,
                    )
                    if (!singleHandDragging) {
                        val moved = sqrt(
                            square(delta.x) + square(delta.y) + square(delta.z),
                        )
                        if (moved >= PANEL_DRAG_SLOP) {
                            singleHandDragging = true
                            Log.i(
                                INTERACTION_TAG,
                                "${logName}DragStart action=move source=ui",
                            )
                        }
                    }
                    if (singleHandDragging) {
                        accumulatedMove = Vector3(
                            x = accumulatedMove.x + delta.x * metersPerPixel,
                            y = accumulatedMove.y - delta.y * metersPerPixel,
                            z = accumulatedMove.z + delta.z * metersPerPixel,
                        )
                        currentOnMove(
                            Vector3(
                                x = delta.x * metersPerPixel,
                                y = -delta.y * metersPerPixel,
                                z = delta.z * metersPerPixel,
                            ),
                        )
                        consume = true
                    }
                }
            }
            consume
        }
    }
}


@Composable
private fun Modifier.setupPanelDrag(
    panelEntity: Entity?,
    onPanelPositionChange: (Vector3) -> Unit,
): Modifier {
    if (panelEntity == null) return this

    val context = LocalContext.current
    val physicalLengthConverter = LocalPhysicalLengthConverter.current
    val density = LocalDensity.current
    val metersPerPixel =
        physicalLengthConverter.dpToLength(1.dp, LengthUnit.Meters) / density.density
    val onMove by rememberUpdatedState(onPanelPositionChange)

    return pointerInput(panelEntity) {
        var loggedFirstMove = false
        detectSpatialDragGesture(
            context = context,
            targetedToEntity = null,
            onDragStart = { _, dragValue ->
                Log.i(
                    INTERACTION_TAG,
                    "setupPanelDragStartV2 kind=${dragValue.interactionKind}",
                )
            },
            onDrag = { dragValue ->
                val move = Vector3(
                    x = dragValue.dragAmount.x * metersPerPixel,
                    y = -dragValue.dragAmount.y * metersPerPixel,
                    z = 0f,
                )
                onMove(move)
                if (!loggedFirstMove) {
                    loggedFirstMove = true
                    Log.i(
                        INTERACTION_TAG,
                        "setupPanelDragMoveV2 delta=${dragValue.dragAmount.x}," +
                            "${dragValue.dragAmount.y},${dragValue.dragAmount.z}",
                    )
                }
            },
            onDragEnd = {
                Log.i(INTERACTION_TAG, "setupPanelDragEndV2")
            },
            onDragCancel = {
                Log.i(INTERACTION_TAG, "setupPanelDragCancelV2")
        },
        )
    }
}

/** Product-level interaction modes. Casual is the first implemented mode. */
private enum class GameMode {
    CASUAL,
    FEEL,
    TIMED,
}

private enum class DifficultyChoice {
    PRESET_3,
    PRESET_4,
    PRESET_5,
    CUSTOM,
}

private data class BoardSpec(
    val rows: Int,
    val cols: Int,
    val layers: Int,
) {
    val cellCount: Int = rows * cols * layers
    val pairCount: Int = cellCount / 2
    val hasBlankCell: Boolean = cellCount % 2 == 1
    val label: String = "${rows}×$cols×$layers"
    val nextLevel: BoardSpec
        get() = BoardSpec(rows + 1, cols + 1, layers + 1)

    companion object {
        val EASY = BoardSpec(3, 3, 3)
        val NORMAL = BoardSpec(4, 4, 4)
        val HARD = BoardSpec(5, 5, 5)
    }
}

private data class FloatingControlSpec(
    val id: String,
    val position: Vector3,
    val phase: Float,
    val color: Color4,
)

/**
 * A detached control arc in front of the board. These panels are not children
 * of the rotating board, so they stay reachable while the cube orbits.
 */
private val FLOATING_CONTROLS = listOf(
    FloatingControlSpec(
        id = "control_hint",
        position = Vector3(-0.110f, CONTROL_ROW_Y, CONTROL_ROW_Z),
        phase = 0f,
        color = Color4(1.00f, 0.88f, 0.54f, 1f),
    ),
    FloatingControlSpec(
        id = "control_shuffle",
        position = Vector3(0f, CONTROL_ROW_Y, CONTROL_ROW_Z),
        phase = 2.1f,
        color = Color4(0.54f, 0.80f, 1.00f, 1f),
    ),
    FloatingControlSpec(
        id = "control_give_up",
        position = Vector3(0.110f, CONTROL_ROW_Y, CONTROL_ROW_Z),
        phase = 5.3f,
        color = Color4(1.00f, 0.62f, 0.62f, 1f),
    ),
)

private data class AnimalCardStyle(
    val emoji: String,
    val baseColor: Color4,
)

private val ANIMAL_EMOJIS = listOf(
    "🐱", // cat
    "🐶", // dog
    "🐰", // rabbit
    "🦊", // fox
    "🐻", // bear
    "🐼", // panda
    "🐸", // frog
    "🐵", // monkey
)

/**
 * Eight animals × eight color editions provide 64 unique identities. Larger
 * levels reuse these identities in complete pairs, so custom boards have no
 * upper dimension limit while every card remains visually distinct enough and
 * legally matchable.
 */
private val CARD_STYLES: List<AnimalCardStyle> = ANIMAL_EMOJIS.flatMap { emoji ->
    listOf(
        Color4(0.76f, 0.23f, 0.24f, 1f),
        Color4(0.86f, 0.50f, 0.19f, 1f),
        Color4(0.18f, 0.52f, 0.34f, 1f),
        Color4(0.15f, 0.39f, 0.72f, 1f),
        Color4(0.77f, 0.42f, 0.70f, 1f),
        Color4(0.31f, 0.70f, 0.72f, 1f),
        Color4(0.89f, 0.69f, 0.29f, 1f),
        Color4(0.49f, 0.50f, 0.61f, 1f),
    ).map { AnimalCardStyle(emoji, it) }
}

/** Low-latency gameplay sounds, celebration effects, and the Chinese win voice. */
private class MatchSoundEffect(context: android.content.Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setMaxStreams(8)
        .build()
    private val successId: Int = soundPool.load(context, R.raw.match_success, 1)
    private val failureId: Int = soundPool.load(context, R.raw.match_failure, 1)
    private val fanfareId: Int = soundPool.load(context, R.raw.celebration_fanfare, 1)
    private val cannonId: Int = soundPool.load(context, R.raw.party_cannon, 1)
    private val fireworkId: Int = soundPool.load(context, R.raw.firework_pop, 1)
    private val uiHoverId: Int = soundPool.load(context, R.raw.ui_hover, 1)
    private val uiClickId: Int = soundPool.load(context, R.raw.ui_click, 1)
    private val cardSelectId: Int = soundPool.load(context, R.raw.card_select, 1)
    private val cardContactId: Int = soundPool.load(context, R.raw.card_contact, 1)
    private val cardLandId: Int = soundPool.load(context, R.raw.card_land, 1)
    private val shuffleWhooshId: Int = soundPool.load(context, R.raw.shuffle_whoosh, 1)
    private val sampleNames: Map<Int, String> = mapOf(
        successId to "match_success",
        failureId to "match_failure",
        fanfareId to "celebration_fanfare",
        cannonId to "party_cannon",
        fireworkId to "firework_pop",
        uiHoverId to "ui_hover",
        uiClickId to "ui_click",
        cardSelectId to "card_select",
        cardContactId to "card_contact",
        cardLandId to "card_land",
        shuffleWhooshId to "shuffle_whoosh",
    )
    private var successReady = false
    private var failureReady = false
    private var fanfareReady = false
    private var cannonReady = false
    private var fireworkReady = false
    private var uiHoverReady = false
    private var uiClickReady = false
    private var cardSelectReady = false
    private var cardContactReady = false
    private var cardLandReady = false
    private var shuffleWhooshReady = false
    private var textToSpeech: TextToSpeech? = null
    private var voiceReady = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val sampleName = sampleNames[sampleId] ?: "unknown"
            Log.i(
                INTERACTION_TAG,
                "soundLoad sample=$sampleName id=$sampleId status=$status",
            )
            if (status == 0) {
                when (sampleId) {
                    successId -> successReady = true
                    failureId -> failureReady = true
                    fanfareId -> fanfareReady = true
                    cannonId -> cannonReady = true
                    fireworkId -> fireworkReady = true
                    uiHoverId -> uiHoverReady = true
                    uiClickId -> uiClickReady = true
                    cardSelectId -> cardSelectReady = true
                    cardContactId -> cardContactReady = true
                    cardLandId -> cardLandReady = true
                    shuffleWhooshId -> shuffleWhooshReady = true
                }
            }
        }

        textToSpeech = TextToSpeech(context) { status ->
            val engine = textToSpeech
            voiceReady = status == TextToSpeech.SUCCESS && engine != null
        }
    }

    fun success(combo: Int = 0) {
        if (successReady) {
            val rate = (0.96f + min(combo, 8) * 0.045f).coerceAtMost(1.38f)
            playSample(successId, "match_success", 0.9f, priority = 1, rate = rate)
        }
    }

    fun failure() {
        if (failureReady) {
            playSample(failureId, "match_failure", 0.8f, priority = 1)
        }
    }

    fun celebration(
        message: String,
        language: Locale,
    ) {
        if (fanfareReady) {
            playSample(fanfareId, "celebration_fanfare", 1f, priority = 1)
        }
        if (voiceReady) {
            textToSpeech?.let { engine ->
                val languageResult = engine.setLanguage(language)
                if (
                    languageResult != TextToSpeech.LANG_MISSING_DATA &&
                        languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    engine.speak(
                        message,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "linklin_win_voice",
                    )
                }
            }
        }
    }

    fun partyCannon() {
        if (cannonReady) {
            playSample(cannonId, "party_cannon", 0.95f, priority = 1)
        }
    }

    fun firework() {
        if (fireworkReady) {
            playSample(fireworkId, "firework_pop", 0.78f, priority = 1)
        }
    }

    fun uiHover() {
        if (uiHoverReady) {
            playSample(uiHoverId, "ui_hover", 0.34f, priority = 0)
        }
    }

    fun uiClick() {
        if (uiClickReady) {
            playSample(uiClickId, "ui_click", 0.55f, priority = 1)
        }
    }

    fun cardSelect() {
        if (cardSelectReady) {
            playSample(cardSelectId, "card_select", 0.40f, priority = 1)
        }
    }

    fun cardContact() {
        if (cardContactReady) {
            playSample(cardContactId, "card_contact", 0.58f, priority = 1)
        }
    }

    fun cardLand() {
        if (cardLandReady) {
            playSample(cardLandId, "card_land", 0.40f, priority = 0)
        }
    }

    fun shuffleWhoosh() {
        if (shuffleWhooshReady) {
            playSample(shuffleWhooshId, "shuffle_whoosh", 0.62f, priority = 1)
        }
    }

    private fun playSample(
        sampleId: Int,
        sampleName: String,
        volume: Float,
        priority: Int,
        rate: Float = 1f,
    ) {
        val streamId = soundPool.play(sampleId, volume, volume, priority, 0, rate)
        Log.i(
            INTERACTION_TAG,
            "soundPlay sample=$sampleName id=$sampleId stream=$streamId " +
                "volume=$volume rate=$rate",
        )
    }

    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        soundPool.release()
    }
}

/**
 * Metric layout for the selected board and status panel. The board is a real
 * cube with gaps on all three axes and remains inside the volumetric window.
 */
private data class BoardLayout(
    val containerMeters: Float,
    val rows: Int,
    val cols: Int,
    val layers: Int,
    val cardSize: Float,
    val gap: Float,
    val rotationFitScale: Float,
    val boardCenterY: Float,
    val panelCenterY: Float,
    val panelWidth: Float,
    val panelHeight: Float,
) {
    fun gridPosition(r: Int, c: Int, l: Int): Vector3 {
        val step = cardSize + gap
        return Vector3(
            (c - (cols - 1) / 2f) * step,
            boardCenterY + ((rows - 1) / 2f - r) * step,
            (l - (layers - 1) / 2f) * step,
        )
    }

    companion object {
        fun fromContainerSize(
            containerMeters: Float,
            rows: Int = GRID_ROWS,
            cols: Int = GRID_COLS,
            layers: Int = GRID_LAYERS,
        ): BoardLayout {
            val half = containerMeters / 2f
            val margin = 0.03f
            val bottomExtraMargin = 0.02f
            val selectionExpansionMargin = 0.015f
            // Large custom cubes must remain playable. Once the edge exceeds the
            // hand-authored presets, use a tighter gap and let the Full Stage
            // expand around the board instead of shrinking cards into mud.
            val largestDimension = maxOf(rows, cols, layers)
            val gap = if (largestDimension <= 5) 0.04f else 0.030f
            val panelWidth = 0.40f
            val panelHeight = 0.175f

            val panelCenterY = half - margin - panelHeight / 2f
            val panelBottom = panelCenterY - panelHeight / 2f - gap
            val bottom = -half + margin + bottomExtraMargin

            val byWidth = (2f * (half - margin) - (cols - 1) * gap) / cols
            val byHeight =
                ((panelBottom - bottom) - (rows - 1) * gap - selectionExpansionMargin) /
                    rows
            val byDepth = (2f * (half - margin) - (layers - 1) * gap) / layers
            // The old formula was designed to fit every cube inside the retired
            // 960dp volumetric window. Full Space has no such hard boundary, so
            // preserve a readable floor for direct pointing and hand tracking.
            val fittedCardSize = min(min(byWidth, byHeight), byDepth)
            val cardSize = maxOf(fittedCardSize, MIN_PLAYABLE_CARD_SIZE)
            val boardExtentX = (cols - 1) * gap + cardSize
            val boardExtentY = (rows - 1) * gap + cardSize
            val boardExtentZ = (layers - 1) * gap + cardSize
            val circumscribedSphereRadius = sqrt(
                square(boardExtentX / 2f) +
                    square(boardExtentY / 2f) +
                    square(boardExtentZ / 2f),
            )
            // Keep the root at unit scale. Custom boards can grow beyond the
            // original window; rotating or stepping back is preferable to making
            // cards impossible to tap.
            val rotationFitScale = 1f

            return BoardLayout(
                containerMeters = containerMeters,
                rows = rows,
                cols = cols,
                layers = layers,
                cardSize = cardSize,
                gap = gap,
                rotationFitScale = rotationFitScale,
                boardCenterY = (bottom + panelBottom) / 2f,
                panelCenterY = panelCenterY,
                panelWidth = panelWidth,
                panelHeight = panelHeight,
            )
        }
    }
}

private fun Color4.toAndroidColor(): Int =
    android.graphics.Color.argb(
        (alpha * 255f).toInt().coerceIn(0, 255),
        (red * 255f).toInt().coerceIn(0, 255),
        (green * 255f).toInt().coerceIn(0, 255),
        (blue * 255f).toInt().coerceIn(0, 255),
    )

private fun square(value: Float): Float = value * value

private fun Color4.toComposeColor(): Color =
    Color(red = red, green = green, blue = blue, alpha = alpha)

private fun Color4.shifted(
    target: Color4,
    fraction: Float,
): Color4 = Color4(
    red + (target.red - red) * fraction,
    green + (target.green - green) * fraction,
    blue + (target.blue - blue) * fraction,
    alpha,
)

private fun Color4.lightened(fraction: Float) = shifted(Color4(1f, 1f, 1f, 1f), fraction)
private fun Color4.darkened(fraction: Float) = shifted(Color4(0f, 0f, 0f, 1f), fraction)

/**
 * A compact starfield used on the interior faces of the cosmos enclosure. The
 * palette stays close to black while retaining a few blue and gold highlights
 * so the volumetric scene remains readable rather than becoming a void.
 */
private fun createCosmosTexture(size: Int = 2048): TextureResource {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val random = Random(20260919L)

    canvas.drawColor(android.graphics.Color.rgb(4, 4, 8))
    val background = Paint().apply {
        shader = LinearGradient(
            0f,
            0f,
            size * 0.25f,
            size.toFloat(),
            android.graphics.Color.rgb(4, 4, 9),
            android.graphics.Color.rgb(9, 9, 20),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), background)

    // Very subtle nebulae: enough depth to feel cosmic, but still black.
    listOf(
        Triple(0.24f, 0.32f, 0.42f) to android.graphics.Color.argb(26, 70, 108, 178),
        Triple(0.76f, 0.64f, 0.48f) to android.graphics.Color.argb(22, 142, 86, 178),
        Triple(0.50f, 0.84f, 0.34f) to android.graphics.Color.argb(16, 190, 116, 72),
    ).forEach { (center, color) ->
        val paint = Paint().apply {
            shader = RadialGradient(
                size * center.first,
                size * center.second,
                size * center.third,
                color,
                android.graphics.Color.argb(0, 0, 0, 0),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    }

    val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    repeat(1800) {
        val x = random.nextFloat() * size
        val y = random.nextFloat() * size
        val radius = when {
            random.nextFloat() < 0.08f -> 1.8f + random.nextFloat() * 2.0f
            random.nextFloat() < 0.34f -> 1.0f + random.nextFloat() * 1.2f
            else -> 0.45f + random.nextFloat() * 0.75f
        }
        val alpha = when {
            radius > 2.4f -> 190 + random.nextInt(56)
            radius > 1.4f -> 140 + random.nextInt(88)
            else -> 72 + random.nextInt(120)
        }
        val color = when {
            random.nextFloat() < 0.10f -> android.graphics.Color.rgb(168, 216, 255)
            random.nextFloat() < 0.08f -> android.graphics.Color.rgb(255, 232, 184)
            else -> android.graphics.Color.WHITE
        }
        starPaint.color = android.graphics.Color.argb(
            alpha.coerceIn(72, 255),
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color),
        )
        canvas.drawCircle(x, y, radius, starPaint)
    }

    return TextureResource.create(bitmap)
}

/**
 * A soft cartoon texture: mostly solid macaron color, a barely visible
 * top-to-bottom gradient, and a deeper bottom edge that reads as thickness.
 */
private fun createControlTexture(color: Color4): TextureResource {
    val width = 192
    val height = 160
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color.toAndroidColor())

    val gradient = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            color.lightened(0.05f).toAndroidColor(),
            color.darkened(0.05f).toAndroidColor(),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradient)

    val bottomEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.darkened(0.18f).toAndroidColor()
    }
    canvas.drawRect(0f, height - 18f, width.toFloat(), height.toFloat(), bottomEdge)
    val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = 5f
        this.color = color.darkened(0.10f).toAndroidColor()
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), edge)

    return TextureResource.create(bitmap)
}

private data class DirectMenuLabels(
    val title: String,
    val subtitle: String,
    val rule: String,
    val casualLine: String,
    val menuHint: String,
    val start: String,
    val exit: String,
    val language: String,
)

private fun createDirectMenuBackdropTexture(labels: DirectMenuLabels): TextureResource {
    val width = 1024
    val height = 768
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.argb(242, 5, 10, 22))

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = android.graphics.Color.argb(150, 138, 203, 255)
    }
    canvas.drawRoundRect(8f, 8f, width - 8f, height - 8f, 54f, 54f, border)

    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 82f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(labels.title, width / 2f, 126f, title)

    val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(169, 184, 206)
        textSize = 42f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(labels.subtitle, width / 2f, 205f, subtitle)
    canvas.drawText(labels.rule, width / 2f, 267f, subtitle)

    val section = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(169, 195, 232)
        textSize = 38f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(labels.casualLine, width / 2f, 355f, section)
    canvas.drawText(labels.menuHint, width / 2f, 412f, section)

    return TextureResource.create(bitmap)
}

private fun createDirectMenuButtonTexture(
    text: String,
    accent: Int,
    selected: Boolean = false,
): TextureResource {
    val width = 640
    val height = 200
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(
        if (selected) {
            android.graphics.Color.argb(238, accent, accent, accent)
        } else {
            android.graphics.Color.argb(238, 16, 26, 43)
        },
    )
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = if (selected) 12f else 7f
        color = accent
    }
    canvas.drawRoundRect(8f, 8f, width - 8f, height - 8f, 44f, 44f, border)
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 68f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val center = height / 2f - (label.ascent() + label.descent()) / 2f
    canvas.drawText(text, width / 2f, center, label)
    return TextureResource.create(bitmap)
}

private fun createComboTexture(text: String): TextureResource {
    val width = 640
    val height = 256
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = android.graphics.Color.argb(185, 255, 210, 128)
    }
    canvas.drawRoundRect(8f, 8f, width - 8f, height - 8f, 58f, 58f, border)

    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 82f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val metrics = title.fontMetrics
    val centerY = height / 2f - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText(text, width / 2f, centerY, title)

    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(52, 255, 226, 150)
    }
    canvas.drawRoundRect(18f, 18f, width - 18f, height - 18f, 48f, 48f, glow)
    return TextureResource.create(bitmap)
}

/** Soft elliptical projection used beneath every floating UI element. */
private fun createSoftShadowTexture(): TextureResource {
    val width = 256
    val height = 160
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            width / 2f,
            height / 2f,
            width / 2f,
            android.graphics.Color.argb(88, 34, 38, 46),
            android.graphics.Color.argb(0, 34, 38, 46),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return TextureResource.create(bitmap)
}

/**
 * Bakes the pair color and animal into one opaque texture. The cube UV layout
 * repeats this complete texture on all six faces, so color and emoji can never
 * separate or sort independently during rotation.
 */
private fun createCompleteCardTexture(
    style: AnimalCardStyle,
    selected: Boolean = false,
): TextureResource {
    val size = CARD_TEXTURE_SIZE
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val backgroundColor = if (selected) {
        style.baseColor.lightened(0.26f)
    } else {
        style.baseColor
    }
    canvas.drawColor(backgroundColor.toAndroidColor())

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = size * if (selected) 0.058f else 0.025f
        color = android.graphics.Color.argb(
            if (selected) 240 else 72,
            255,
            255,
            255,
        )
    }
    val borderInset = size * if (selected) 0.022f else 0.035f
    canvas.drawRoundRect(
        borderInset,
        borderInset,
        size - borderInset,
        size - borderInset,
        size * 0.10f,
        size * 0.10f,
        border,
    )

    val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = size * 0.66f
        typeface = Typeface.DEFAULT
    }
    val metrics = emojiPaint.fontMetrics
    val centerY = size / 2f - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText(style.emoji, size / 2f, centerY, emojiPaint)

    return TextureResource.create(bitmap)
}

private data class MovingCard(
    val card: ModelEntity,
    val source: Cell,
    val target: Cell,
)

/**
 * A direct-3D setup menu. This avoids AttachmentPanel composition, which the
 * PICO emulator can drop after repeated Stage recreation.
 */
private class DirectSetupMenuSceneState {
    private var built = false
    private var rootEntity: Entity? = null
    private val buttons = LinkedHashMap<String, ModelEntity>()
    private val meshes = ArrayList<MeshResource>()
    private val textures = ArrayList<TextureResource>()
    private val materials = ArrayList<UnlitMaterial>()

    val root: Entity?
        get() = rootEntity

    fun build(labels: DirectMenuLabels) {
        if (built) return
        val root = Entity().apply { setName("direct_setup_menu_root") }
        rootEntity = root

        val backdropMesh = MeshResource.createBox(
            Vector3(0.560f, 0.420f, 0.018f),
            cornerRadius = 0.038f,
        ).apply {
            toGlobal()
            meshes += this
        }
        val backdropTexture = createDirectMenuBackdropTexture(labels).apply {
            toGlobal()
            textures += this
        }
        val backdropMaterial = UnlitMaterial.create().apply {
            setBaseColorTexture(backdropTexture)
            setBaseColor(Color4(1f, 1f, 1f, 1f))
            setApplyToneMapping(false)
            toGlobal()
            materials += this
        }
        val backdrop = ModelEntity(backdropMesh, backdropMaterial).apply {
            setName("direct_setup_menu_backdrop")
        }
        root.addChild(backdrop)

        fun addButton(
            id: String,
            label: String,
            accent: Int,
            position: Vector3,
            size: Vector3,
        ) {
            val mesh = MeshResource.createBox(
                size = size,
                cornerRadius = 0.020f,
            ).apply {
                toGlobal()
                meshes += this
            }
            val texture = createDirectMenuButtonTexture(
                text = label,
                accent = accent,
            ).apply {
                toGlobal()
                textures += this
            }
            val material = UnlitMaterial.create().apply {
                setBaseColorTexture(texture)
                setBaseColor(Color4(1f, 1f, 1f, 1f))
                setApplyToneMapping(false)
                toGlobal()
                materials += this
            }
            val button = ModelEntity(mesh, material).apply {
                setName("direct_setup_$id")
                components.set(InteractableComponent())
                components.set(HoverEffectComponent())
                components.set(
                    CollisionComponent(
                        collisionShape = listOf(
                            ShapeResource.createBox(
                                Vector3(size.x * 1.04f, size.y * 1.08f, size.z * 1.2f),
                            ),
                        ),
                        physicsMaterial = PhysicsMaterialResource(),
                    ),
                )
                components[TransformComponent::class.java]?.setPosition(position)
            }
            root.addChild(button)
            buttons[id] = button
        }

        addButton(
            id = "start",
            label = labels.start,
            accent = android.graphics.Color.rgb(255, 179, 71),
            position = Vector3(-0.105f, -0.165f, 0.026f),
            size = Vector3(0.210f, 0.062f, 0.016f),
        )
        addButton(
            id = "exit",
            label = labels.exit,
            accent = android.graphics.Color.rgb(255, 158, 158),
            position = Vector3(0.145f, -0.165f, 0.026f),
            size = Vector3(0.155f, 0.062f, 0.016f),
        )
        addButton(
            id = "language",
            label = labels.language,
            accent = android.graphics.Color.rgb(138, 203, 255),
            position = Vector3(0f, -0.085f, 0.026f),
            size = Vector3(0.210f, 0.054f, 0.016f),
        )

        root.components[TransformComponent::class.java]?.apply {
            setPosition(DIRECT_SETUP_PANEL_DEFAULT_POSITION)
            setEulerAngles(EulerAngles())
        }
        built = true
    }

    fun action(entity: Entity?): String? =
        buttons.entries.firstOrNull { it.value == entity }?.key

    fun destroy() {
        rootEntity?.destroy()
        rootEntity = null
        buttons.clear()
        materials.forEach { it.close() }
        textures.forEach { it.close() }
        meshes.forEach { it.close() }
        materials.clear()
        textures.clear()
        meshes.clear()
        built = false
    }
}

private data class CelebrationEvent(
    val atMillis: Float,
    val center: Vector3,
    val count: Int,
    val minSpeed: Float,
    val maxSpeed: Float,
    val lifeSeconds: Float,
    val gravity: Float,
)

private data class CelebrationCannon(
    val entity: ModelEntity,
    val basePosition: Vector3,
    val basePitch: Float,
    val baseYaw: Float,
)

/**
 * A reusable 3D backdrop for the setup sheet. The Compose layer supplies the
 * readable cosmic surface; this opaque depth plate keeps the board behind it.
 */
private class SetupPanelSceneState {
    private var body: ModelEntity? = null
    private var shadow: ModelEntity? = null
    private var mesh: MeshResource? = null
    private var texture: TextureResource? = null
    private var material: UnlitMaterial? = null
    private var shadowMesh: MeshResource? = null
    private var shadowTexture: TextureResource? = null
    private var shadowMaterial: UnlitMaterial? = null

    fun attach(panel: Entity) {
        if (body != null) return

        val bodyMesh = MeshResource.createBox(
            size = Vector3(
                SETUP_PANEL_WIDTH + 0.026f,
                SETUP_PANEL_HEIGHT + 0.026f,
                SETUP_PANEL_DEPTH,
            ),
            cornerRadius = SETUP_PANEL_CORNER_RADIUS,
        ).apply { toGlobal() }
        val bodyTexture = createControlTexture(
            Color4(0.035f, 0.055f, 0.105f, 1.0f),
        ).apply { toGlobal() }
        val bodyMaterial = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
            setBaseColorTexture(bodyTexture)
            setBaseColor(Color4(0.82f, 0.90f, 1.00f, 1f))
            setOpacity(0.94f)
            setDepthWrite(true)
            setApplyToneMapping(false)
            toGlobal()
        }
        val panelBody = ModelEntity(bodyMesh, bodyMaterial).apply {
            setName("setup_panel_glass_body")
            components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        x = 0f,
                        y = 0f,
                        z = -SETUP_PANEL_DEPTH / 2f - 0.004f,
                    ),
                )
                setEulerAngles(EulerAngles())
                setScaleVector(Vector3(1f, 1f, 1f))
            }
        }

        val softShadowMesh = MeshResource.createPlane(1f, 1f).apply { toGlobal() }
        val softShadowTexture = createSoftShadowTexture().apply { toGlobal() }
        val softShadowMaterial = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
            setBaseColorTexture(softShadowTexture)
            setBaseColor(Color4(0.28f, 0.46f, 0.85f, 1f))
            setOpacity(0.34f)
            setDepthWrite(false)
            setApplyToneMapping(false)
            toGlobal()
        }
        val panelShadow = ModelEntity(softShadowMesh, softShadowMaterial).apply {
            setName("setup_panel_soft_shadow")
            components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        x = 0f,
                        y = -SETUP_PANEL_HEIGHT * 0.12f,
                        z = -SETUP_PANEL_DEPTH - 0.012f,
                    ),
                )
                setEulerAngles(EulerAngles())
                setScaleVector(
                    Vector3(
                        (SETUP_PANEL_WIDTH + 0.026f) * 1.16f,
                        (SETUP_PANEL_HEIGHT + 0.026f) * 1.26f,
                        1f,
                    ),
                )
            }
        }

        panel.addChild(panelBody)
        panel.addChild(panelShadow)
        body = panelBody
        shadow = panelShadow
        mesh = bodyMesh
        texture = bodyTexture
        material = bodyMaterial
        shadowMesh = softShadowMesh
        shadowTexture = softShadowTexture
        shadowMaterial = softShadowMaterial
    }

    fun destroy() {
        body?.destroy()
        shadow?.destroy()
        body = null
        shadow = null

        material?.close()
        texture?.close()
        mesh?.close()
        shadowMaterial?.close()
        shadowTexture?.close()
        shadowMesh?.close()
        mesh = null
        texture = null
        material = null
        shadowMesh = null
        shadowTexture = null
        shadowMaterial = null
    }
}

/**
 * A large inward-facing starfield sphere for the full-space Stage. Front-face
 * culling renders its interior from every direction without occluding the
 * board or controls.
 */
private class CosmosBackgroundSceneState {
    val root: Entity = Entity().apply { setName("linklin_cosmos_background") }

    private var background: ModelEntity? = null
    private var mesh: MeshResource? = null
    private var texture: TextureResource? = null
    private var material: UnlitMaterial? = null

    init {
        val backgroundMesh = MeshResource.createSphere(
            radius = COSMOS_BACKGROUND_RADIUS,
        ).apply { toGlobal() }
        val backgroundTexture = createCosmosTexture().apply { toGlobal() }
        val backgroundMaterial = UnlitMaterial.create().apply {
            setBaseColorTexture(backgroundTexture)
            setBaseColor(Color4(1f, 1f, 1f, 1f))
            setCullingMode(MaterialCullingMode.NONE)
            setDepthTest(true)
            // Keep depth writing enabled. On PICO hardware the omnidirectional
            // sphere disappears into a black layer without depth ownership.
            setDepthWrite(true)
            setApplyToneMapping(false)
            toGlobal()
        }
        val backgroundEntity = ModelEntity(backgroundMesh, backgroundMaterial).apply {
            setName("linklin_cosmos_interior")
            components[TransformComponent::class.java]?.apply {
                setPosition(Vector3(0f, 0f, 0f))
                setEulerAngles(EulerAngles())
                setScaleVector(Vector3(1f, 1f, 1f))
            }
        }
        root.addChild(backgroundEntity)

        background = backgroundEntity
        mesh = backgroundMesh
        texture = backgroundTexture
        material = backgroundMaterial
    }

    fun destroy() {
        root.destroy()
        material?.close()
        texture?.close()
        mesh?.close()
        material = null
        texture = null
        mesh = null
        background = null
    }

    fun update(deltaSeconds: Float) {
        background?.components?.get(TransformComponent::class.java)?.apply {
            val nextYaw = eulerAngles.yaw + deltaSeconds * 0.55f
            setEulerAngles(EulerAngles(yaw = nextYaw % 360f))
        }
    }
}

private suspend fun animateFloatingControls(
    controls: List<Pair<FloatingControlSpec, Entity>>,
) {
    val startAt = System.nanoTime()

    while (true) {
        if (controls.any { !it.second.valid }) return

        val elapsed = (System.nanoTime() - startAt) / 1_000_000f
        controls.forEach { (spec, entity) ->
            val wave = sin(
                2.0 * PI * elapsed / CONTROL_BOB_DURATION_MS + spec.phase,
            ).toFloat()
            entity.components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        x = spec.position.x,
                        y = spec.position.y + 0.0075f * wave,
                        z = spec.position.z + 0.004f * sin(wave),
                    ),
                )
                setEulerAngles(
                    EulerAngles(
                        pitch = -2.4f * wave,
                        yaw = 3.6f * wave,
                    ),
                )
            }
        }
        delay(16)
    }
}

private class GameSceneState(private val layout: BoardLayout) {
    var boardRoot: Entity? = null
    var celebrationRoot: Entity? = null
        private set
    private var cardMesh: MeshResource? = null
    private var cardTextures: List<TextureResource>? = null
    private var cardMaterials: Array<Array<UnlitMaterial>>? = null
    private var boardYaw = INITIAL_BOARD_YAW
    private var boardPitch = 0f
    private var boardPosition = Vector3(
        x = 0f,
        y = STAGE_CONTENT_Y,
        z = STAGE_CONTENT_Z,
    )
    private var boardRows = GRID_ROWS
    private var boardCols = GRID_COLS
    private var boardLayers = GRID_LAYERS

    val cards = LinkedHashMap<Cell, ModelEntity>()
    private val cardKinds = HashMap<Cell, Int>()
    private val floatingControls = LinkedHashMap<String, FloatingControlRuntime>()
    private var controlMesh: MeshResource? = null
    private var controlTextures: List<TextureResource>? = null
    private var controlMaterials: Map<String, UnlitMaterial>? = null
    private var softShadowMesh: MeshResource? = null
    private var softShadowTexture: TextureResource? = null
    private var softShadowMaterial: UnlitMaterial? = null
    private var statusPanelMesh: MeshResource? = null
    private var statusPanelTexture: TextureResource? = null
    private var statusPanelMaterial: UnlitMaterial? = null
    private var statusPanelBody: ModelEntity? = null
    private var statusPanelShadow: ModelEntity? = null
    private var feelGrab: FeelGrabState? = null
    private var secondFeelGrab: FeelGrabState? = null
    private var feelTargetCell: Cell? = null
    private val feelReturnGenerations = HashMap<Cell, Long>()
    private var feelReturnGeneration = 0L
    private var celebrationMesh: MeshResource? = null
    private var cannonMesh: MeshResource? = null
    private var cannonMaterials: List<UnlitMaterial> = emptyList()
    private var celebrationMaterials: List<UnlitMaterial> = emptyList()
    private val celebrationParticles = ArrayList<CelebrationParticle>()
    private val celebrationCannons = ArrayList<CelebrationCannon>()
    private var celebrationGeneration = 0
    private val effectJob = SupervisorJob()
    private val effectScope = CoroutineScope(Dispatchers.Main + effectJob)
    private var effectRoot: Entity? = null
    private var effectMesh: MeshResource? = null
    private var comboMesh: MeshResource? = null
    private var effectMaterials: List<UnlitMaterial> = emptyList()
    private var energyLinkMaterial: UnlitMaterial? = null
    private val effectParticles = ArrayList<TransientEffectParticle>()
    private val energyLinkSegments = ArrayList<ModelEntity>()
    private var effectUpdateJob: Job? = null

    private inner class TransientEffectParticle(
        val entity: ModelEntity,
    ) {
        var active = false
        var elapsed = 0f
        var life = 1f
        var origin = Vector3(0f, 0f, 0f)
        var velocity = Vector3(0f, 0f, 0f)
        var gravity = 0f
        var scale = 1f
        var spin = 0f

        fun reset() {
            active = false
            entity.enabled = false
        }
    }

    private inner class FloatingControlRuntime(
        val spec: FloatingControlSpec,
        val root: Entity,
        val body: ModelEntity,
        val shadow: ModelEntity,
        initialPosition: Vector3,
    ) {
        var userPosition: Vector3 = initialPosition
        var dragging: Boolean = false
        var hovered: Boolean = false
        var pressed: Boolean = false
        var enabled: Boolean = true
        var animatedScale: Float = 1f
        var animatedLift: Float = 0f
    }

    private inner class FeelGrabState(
        val cell: Cell,
        val card: ModelEntity,
        val home: Vector3,
    ) {
        var current: Vector3
        var target: Vector3
        var velocity: Vector3 = Vector3(0f, 0f, 0f)
        var scale: Float = FEEL_GRAB_SCALE
        var releasing: Boolean = false
        var proximity: Float = 0f
        var repelOffset: Vector3 = Vector3(0f, 0f, 0f)
        var repelUntil: Long = 0L

        init {
            val lifted = home + Vector3(0f, 0f, layout.cardSize * 0.26f)
            current = lifted
            target = lifted
        }
    }

    private inner class CelebrationParticle(
        val entity: ModelEntity,
    ) {
        var active: Boolean = false
        var elapsed: Float = 0f
        var life: Float = 1f
        var origin: Vector3 = Vector3(0f, 0f, 0f)
        var velocity: Vector3 = Vector3(0f, 0f, 0f)
        var gravity: Float = -0.34f
        var scale: Float = 1f
        var spin: Float = 180f

        fun reset() {
            active = false
            entity.enabled = false
        }
    }

    suspend fun build(game: LianliankanGame) {
        check(boardRoot == null) { "Scene must be destroyed before rebuilding" }
        boardRows = game.rows
        boardCols = game.cols
        boardLayers = game.layers

        val size = Vector3(layout.cardSize, layout.cardSize, layout.cardSize)
        val cardMesh = MeshResource.createBox(size).apply { toGlobal() }
        val collider = ShapeResource.createBox(size)
        val styleCount = min(CARD_STYLES.size, game.pairCount)

        // One texture contains both the opaque background and the animal. A
        // second, clearly outlined texture is reserved for selection/hints.
        // Texture upload is expensive in the emulator. Keep bitmap creation
        // and TextureResource creation off the main thread; material/entity
        // setup below resumes on main after loading.
        val textures = withContext(Dispatchers.Default) {
            CARD_STYLES.take(styleCount).flatMap { style ->
                listOf(
                    createCompleteCardTexture(style, selected = false),
                    createCompleteCardTexture(style, selected = true),
                ).map { texture ->
                    texture.apply { toGlobal() }
                }
            }
        }

        val cardMaterials = (0 until styleCount).map { styleIndex ->
            val normalTexture = textures[styleIndex * 2]
            val selectedTexture = textures[styleIndex * 2 + 1]
            arrayOf(
                UnlitMaterial.create().apply {
                    setBaseColor(Color4(1f, 1f, 1f, 1f))
                    setBaseColorTexture(normalTexture)
                    setApplyToneMapping(false)
                    toGlobal()
                },
                UnlitMaterial.create().apply {
                    setBaseColorTexture(selectedTexture)
                    setBaseColor(Color4(1.22f, 1.20f, 1.14f, 1f))
                    setApplyToneMapping(false)
                    toGlobal()
                },
            )
        }.toTypedArray()

        val root = Entity().apply { setName("linklin_board_3d_root") }
        root.components[TransformComponent::class.java]?.apply {
            setPosition(boardPosition)
            setEulerAngles(EulerAngles(pitch = boardPitch, yaw = boardYaw))
            // The card grid is laid out at its readable rest size first, then
            // the complete root is shrunk so its circumscribed sphere remains
            // inside the volumetric container at every yaw/pitch combination.
            setScaleVector(
                Vector3(
                    layout.rotationFitScale,
                    layout.rotationFitScale,
                    layout.rotationFitScale,
                ),
            )
        }

        for (r in 0 until boardRows) {
            for (c in 0 until boardCols) {
                for (l in 0 until boardLayers) {
                    val cell = Cell(r, c, l)
                    val kind = game.board[r][c][l]
                    val card = ModelEntity(
                        cardMesh,
                        cardMaterials[if (kind == LianliankanGame.EMPTY) 0 else kind][0],
                    ).apply {
                        setName("card_${r}_${c}_${l}")
                        components.set(InteractableComponent())
                        components.set(HoverEffectComponent())
                        components.set(
                            CollisionComponent(
                                collisionShape = listOf(collider),
                                physicsMaterial = PhysicsMaterialResource(),
                            ),
                        )
                        components[TransformComponent::class.java]?.setPosition(
                            layout.gridPosition(r, c, l),
                        )
                    }
                    root.addChild(card)
                    cards[cell] = card
                    cardKinds[cell] = kind
                    card.enabled = kind != LianliankanGame.EMPTY
                }
            }
        }

        if (softShadowMesh == null) {
            softShadowMesh = MeshResource.createPlane(1f, 1f).apply { toGlobal() }
            softShadowTexture = createSoftShadowTexture().apply { toGlobal() }
            softShadowMaterial = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
                setBaseColorTexture(requireNotNull(softShadowTexture))
                setOpacity(0.34f)
                setDepthWrite(false)
                setApplyToneMapping(false)
                toGlobal()
            }
        }

        buildEffects(root)
        buildCelebration()
        boardRoot = root
        this.cardMesh = cardMesh
        this.cardTextures = textures
        this.cardMaterials = cardMaterials
        effectUpdateJob = effectScope.launch {
            while (effectRoot?.valid == true) {
                updateEffectParticles(EFFECT_FRAME_MS / 1000f)
                updateEnergyLink()
                delay(EFFECT_FRAME_MS.toLong())
            }
        }
    }

    /**
     * Builds the reusable gameplay feedback pool: star dust, orbit trails,
     * path glow dots, and the two-card energy link.
     */
    private fun buildEffects(root: Entity) {
        if (effectRoot != null) return

        val mesh = MeshResource.createBox(
            Vector3(
                EFFECT_PARTICLE_SIZE,
                EFFECT_PARTICLE_SIZE,
                EFFECT_PARTICLE_SIZE,
            ),
        ).apply { toGlobal() }
        val textMesh = MeshResource.createPlane(1f, 1f).apply { toGlobal() }
        val colors = listOf(
            Color4(1.00f, 0.82f, 0.42f, 1f),
            Color4(0.62f, 0.84f, 1.00f, 1f),
            Color4(0.78f, 0.96f, 0.88f, 1f),
            Color4(1.00f, 0.70f, 0.82f, 1f),
            Color4(0.92f, 0.92f, 1.00f, 1f),
        )
        val materials = colors.map { color ->
            UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
                setBaseColor(color)
                setOpacity(0.90f)
                setDepthWrite(false)
                setApplyToneMapping(false)
                toGlobal()
            }
        }
        val linkMaterial = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
            setBaseColor(Color4(1.00f, 0.86f, 0.48f, 1f))
            setOpacity(0.72f)
            setDepthWrite(false)
            setApplyToneMapping(false)
            toGlobal()
        }
        val effectsRoot = Entity().apply { setName("linklin_gameplay_effects") }

        repeat(EFFECT_PARTICLE_COUNT) { index ->
            val particle = ModelEntity(mesh, materials[index % materials.size]).apply {
                setName("gameplay_effect_$index")
                enabled = false
                components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(0f, -10f, 0f))
                    setEulerAngles(EulerAngles())
                    setScaleVector(Vector3(0.001f, 0.001f, 0.001f))
                }
            }
            effectsRoot.addChild(particle)
            effectParticles.add(TransientEffectParticle(particle))
        }

        repeat(ENERGY_LINK_SEGMENTS) { index ->
            val segment = ModelEntity(mesh, linkMaterial).apply {
                setName("feel_energy_link_$index")
                enabled = false
                components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(0f, -10f, 0f))
                    setEulerAngles(EulerAngles())
                    setScaleVector(Vector3(0.001f, 0.001f, 0.001f))
                }
            }
            effectsRoot.addChild(segment)
            energyLinkSegments += segment
        }

        root.addChild(effectsRoot)
        effectRoot = effectsRoot
        effectMesh = mesh
        comboMesh = textMesh
        effectMaterials = materials
        energyLinkMaterial = linkMaterial
    }

    /**
     * Builds a detached celebration layer. Unlike the board root, it does not
     * inherit board orbit rotation, so fireworks surround the player-facing
     * volume no matter which face of the cube was last visible.
     */
    private fun buildCelebration() {
        if (celebrationRoot != null) return

        val particleMesh = MeshResource.createBox(
            Vector3(
                CELEBRATION_PARTICLE_SIZE,
                CELEBRATION_PARTICLE_SIZE,
                CELEBRATION_PARTICLE_SIZE,
            ),
        ).apply { toGlobal() }
        val particleMaterials = CELEBRATION_COLORS.map { color ->
            UnlitMaterial.create().apply {
                setBaseColor(color)
                setApplyToneMapping(false)
                toGlobal()
            }
        }
        val root = Entity().apply {
            setName("linklin_celebration_root")
            enabled = false
        }

        repeat(CELEBRATION_PARTICLE_COUNT) { index ->
            val particle = ModelEntity(
                particleMesh,
                particleMaterials[index % particleMaterials.size],
            ).apply {
                setName("celebration_particle_$index")
                enabled = false
                components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(0f, -10f, 0f))
                    setEulerAngles(EulerAngles())
                    setScaleVector(Vector3(0.001f, 0.001f, 0.001f))
                }
            }
            root.addChild(particle)
            celebrationParticles.add(CelebrationParticle(particle))
        }

        val barrelMesh = MeshResource.createBox(
            size = Vector3(0.036f, 0.112f, 0.036f),
            cornerRadius = 0.014f,
        ).apply { toGlobal() }
        val barrelMaterials = listOf(
            Color4(0.97f, 0.48f, 0.38f, 1f),
            Color4(0.41f, 0.68f, 0.98f, 1f),
            Color4(1.00f, 0.80f, 0.30f, 1f),
        ).map { color ->
            UnlitMaterial.create().apply {
                setBaseColor(color)
                setApplyToneMapping(false)
                toGlobal()
            }
        }
        val cannonSpecs = listOf(
            Triple(Vector3(-0.250f, -0.245f, 0.150f), -48f to -24f, 0),
            Triple(Vector3(0.250f, -0.245f, 0.150f), -48f to 24f, 1),
            Triple(Vector3(0f, -0.285f, 0.180f), -54f to 0f, 2),
        )
        cannonSpecs.forEachIndexed { index, (position, angles, materialIndex) ->
            val cannon = ModelEntity(barrelMesh, barrelMaterials[materialIndex]).apply {
                setName("party_cannon_$index")
                enabled = true
                components[TransformComponent::class.java]?.apply {
                    setPosition(position)
                    setEulerAngles(EulerAngles(pitch = angles.first, yaw = angles.second))
                    setScaleVector(Vector3(1f, 1f, 1f))
                }
            }
            root.addChild(cannon)
            celebrationCannons.add(
                CelebrationCannon(
                    entity = cannon,
                    basePosition = position,
                    basePitch = angles.first,
                    baseYaw = angles.second,
                ),
            )
        }

        celebrationRoot = root
        celebrationMesh = particleMesh
        cannonMesh = barrelMesh
        celebrationMaterials = particleMaterials
        cannonMaterials = barrelMaterials
    }

    /** Reuses entities and swaps their six-face sticker materials after restart. */
    fun reset(game: LianliankanGame) {
        val cardMaterials = requireNotNull(cardMaterials) { "scene resources are released" }

        cancelFeelGrab()
        cancelSecondFeelGrab()

        for (r in 0 until game.rows) {
            for (c in 0 until game.cols) {
                for (l in 0 until game.layers) {
                    val cell = Cell(r, c, l)
                    val kind = game.board[cell.r][cell.c][cell.l]
                    val card = requireNotNull(cards[cell]) { "card ${cell} is missing" }
                    cardKinds[cell] = kind
                    card.enabled = true
                    card.components[ModelComponent::class.java]?.materials?.set(
                        index = 0,
                        material = if (kind == LianliankanGame.EMPTY) {
                            cardMaterials[0][0]
                        } else {
                            cardMaterials[kind][0]
                        },
                    )
                    card.components[TransformComponent::class.java]?.apply {
                        setScaleVector(Vector3(1f, 1f, 1f))
                        setPosition(layout.gridPosition(cell.r, cell.c, cell.l))
                    }
                    card.enabled = kind != LianliankanGame.EMPTY
                }
            }
        }
        setBoardRotation(boardYaw, boardPitch)
    }

    val feelGrabbedCell: Cell?
        get() = feelGrab?.cell

    val hasFloatingControls: Boolean
        get() = floatingControls.isNotEmpty()

    fun attachFloatingControls(
        panels: List<Pair<FloatingControlSpec, Entity>>,
        savedPositions: Map<String, Vector3>,
    ) {
        if (floatingControls.isNotEmpty()) return

        val mesh = MeshResource.createBox(
            size = Vector3(CONTROL_WIDTH, CONTROL_HEIGHT, CONTROL_DEPTH),
            cornerRadius = CONTROL_CORNER_RADIUS,
        ).apply { toGlobal() }
        val collider = ShapeResource.createBox(
            Vector3(
                CONTROL_WIDTH * 1.04f,
                CONTROL_HEIGHT * 1.08f,
                CONTROL_DEPTH * 1.20f,
            ),
        )
        val textures = FLOATING_CONTROLS.map { spec ->
            createControlTexture(spec.color).apply { toGlobal() }
        }
        val materials = FLOATING_CONTROLS
            .mapIndexed { index, spec ->
                spec.id to UnlitMaterial.create().apply {
                    setBaseColorTexture(textures[index])
                    setApplyToneMapping(false)
                    toGlobal()
                }
            }
            .toMap()
        val shadowMesh = MeshResource.createPlane(1f, 1f).apply { toGlobal() }
        val shadowTexture = createSoftShadowTexture().apply { toGlobal() }
        val shadowMaterial = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
            setBaseColorTexture(shadowTexture)
            setOpacity(0.34f)
            setDepthWrite(false)
            setApplyToneMapping(false)
            toGlobal()
        }

        panels.forEach { (spec, panel) ->
            panel.setName("floating_control_${spec.id}")
            panel.components.set(InteractableComponent())
            panel.components.set(HoverEffectComponent())
            panel.components.set(
                CollisionComponent(
                    collisionShape = listOf(collider),
                    physicsMaterial = PhysicsMaterialResource(),
                ),
            )
            panel.components[TransformComponent::class.java]?.apply {
                setPosition(savedPositions[spec.id] ?: spec.position)
                setEulerAngles(EulerAngles())
                setScaleVector(Vector3(1f, 1f, 1f))
            }

            val body = ModelEntity(mesh, requireNotNull(materials[spec.id])).apply {
                setName("floating_control_body_${spec.id}")
                components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(0f, 0f, -CONTROL_DEPTH / 2f - 0.0015f))
                    setEulerAngles(EulerAngles())
                    setScaleVector(Vector3(1f, 1f, 1f))
                }
            }
            panel.addChild(body)
            val shadow = ModelEntity(shadowMesh, shadowMaterial).apply {
                setName("floating_control_shadow_${spec.id}")
                components[TransformComponent::class.java]?.apply {
                    setPosition(
                        Vector3(
                            x = 0f,
                            y = -CONTROL_HEIGHT * 0.16f,
                            z = -CONTROL_DEPTH - 0.006f,
                        ),
                    )
                    setEulerAngles(EulerAngles())
                    setScaleVector(
                        Vector3(
                            CONTROL_WIDTH * 1.18f,
                            CONTROL_HEIGHT * 1.34f,
                            1f,
                        ),
                    )
                }
            }
            panel.addChild(shadow)
            floatingControls[spec.id] = FloatingControlRuntime(
                spec = spec,
                root = panel,
                body = body,
                shadow = shadow,
                initialPosition = savedPositions[spec.id] ?: spec.position,
            )
        }

        controlMesh = mesh
        controlTextures = textures
        controlMaterials = materials
        softShadowMesh = shadowMesh
        softShadowTexture = shadowTexture
        softShadowMaterial = shadowMaterial
    }

    fun setControlVisualState(
        id: String,
        hovered: Boolean,
        pressed: Boolean,
    ) {
        floatingControls[id]?.apply {
            this.hovered = hovered
            this.pressed = pressed
            updateControlMaterial(this)
        }
    }

    fun setControlEnabled(
        id: String,
        enabled: Boolean,
    ) {
        floatingControls[id]?.apply {
            this.enabled = enabled
            updateControlMaterial(this)
        }
    }

    fun ensureFloatingControlsVisible() {
        floatingControls.values.forEach { control ->
            control.root.enabled = true
            control.body.enabled = true
            control.shadow.enabled = true
        }
    }

    private fun updateControlMaterial(control: FloatingControlRuntime) {
        val brightness = when {
            !control.enabled -> 0.55f
            control.pressed -> 0.90f
            control.hovered -> 1.08f
            else -> 1f
        }
        controlMaterials?.get(control.spec.id)?.setBaseColor(
            Color4(brightness, brightness, brightness, 1f),
        )
    }

    fun attachStatusGlassBody(panel: Entity) {
        if (statusPanelBody != null) return

        val mesh = MeshResource.createBox(
            size = Vector3(
                layout.panelWidth + 0.024f,
                layout.panelHeight + 0.024f,
                STATUS_PANEL_DEPTH,
            ),
            cornerRadius = STATUS_PANEL_CORNER_RADIUS,
        ).apply { toGlobal() }
        val texture = createControlTexture(
            Color4(0.99f, 0.99f, 1.00f, 1f),
        ).apply { toGlobal() }
        val material = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
            setBaseColorTexture(texture)
            setBaseColor(Color4(1f, 1f, 1f, 1f))
            setOpacity(0.52f)
            setDepthWrite(false)
            setApplyToneMapping(false)
            toGlobal()
        }
        val body = ModelEntity(mesh, material).apply {
            setName("status_panel_glass_body")
            components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        x = 0f,
                        y = 0f,
                        z = -STATUS_PANEL_DEPTH / 2f - 0.003f,
                    ),
                )
                setEulerAngles(EulerAngles())
                setScaleVector(Vector3(1f, 1f, 1f))
            }
        }
        panel.addChild(body)
        val shadow = ModelEntity(
            requireNotNull(softShadowMesh),
            requireNotNull(softShadowMaterial),
        ).apply {
            setName("status_panel_soft_shadow")
            components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        x = 0f,
                        y = -layout.panelHeight * 0.14f,
                        z = -STATUS_PANEL_DEPTH - 0.008f,
                    ),
                )
                setEulerAngles(EulerAngles())
                setScaleVector(
                    Vector3(
                        (layout.panelWidth + 0.024f) * 1.14f,
                        (layout.panelHeight + 0.024f) * 1.30f,
                        1f,
                    ),
                )
            }
        }
        panel.addChild(shadow)

        statusPanelMesh = mesh
        statusPanelTexture = texture
        statusPanelMaterial = material
        statusPanelBody = body
        statusPanelShadow = shadow
    }

    fun isFloatingControl(entity: Entity?): Boolean {
        if (entity == null) return false
        return floatingControls.values.any { control ->
            control.root == entity || control.body == entity
        }
    }

    fun floatingControlId(entity: Entity?): String? =
        floatingControls.values.firstOrNull { control ->
            control.root == entity || control.body == entity
        }?.spec?.id

    fun beginFloatingControlDrag(entity: Entity?): String? {
        val control = floatingControls.values.firstOrNull { control ->
            control.root == entity || control.body == entity
        } ?: return null
        control.dragging = true
        return control.spec.id
    }

    fun moveFloatingControl(
        id: String,
        delta: Vector3,
    ) {
        val control = floatingControls[id] ?: return
        val half = layout.containerMeters / 2f
        val next = control.userPosition + delta
        control.userPosition = Vector3(
            x = next.x.coerceIn(
                -(half - CONTROL_WIDTH / 2f - 0.010f),
                half - CONTROL_WIDTH / 2f - 0.010f,
            ),
            y = next.y.coerceIn(
                -(half - CONTROL_HEIGHT / 2f - 0.010f),
                half - CONTROL_HEIGHT / 2f - 0.010f,
            ),
            z = next.z.coerceIn(
                -(half - CONTROL_DEPTH / 2f - 0.006f),
                half - CONTROL_DEPTH / 2f - 0.006f,
            ),
        )
    }

    fun endFloatingControlDrag(id: String) {
        floatingControls[id]?.dragging = false
    }

    fun floatingControlPositions(): Map<String, Vector3> =
        floatingControls.mapValues { (_, control) ->
            control.userPosition
        }

    suspend fun runFloatingControls() {
        val startAt = System.nanoTime()
        while (true) {
            val elapsed = (System.nanoTime() - startAt) / 1_000_000f
            floatingControls.values.forEach { control ->
                val bob = sin(
                    2.0 * PI * elapsed / CONTROL_BOB_DURATION_MS + control.spec.phase,
                ).toFloat()
                val targetScale = when {
                    control.dragging && !control.pressed -> 1.04f
                    control.pressed -> CONTROL_PRESS_SCALE
                    control.hovered -> CONTROL_HOVER_SCALE
                    else -> 1f
                }
                val targetLift = when {
                    control.dragging && !control.pressed -> 0.010f
                    control.pressed -> CONTROL_PRESS_Z
                    control.hovered -> CONTROL_HOVER_Z
                    else -> 0f
                }
                control.animatedScale += (targetScale - control.animatedScale) * 0.24f
                control.animatedLift += (targetLift - control.animatedLift) * 0.24f

                control.root.components[TransformComponent::class.java]?.apply {
                    setPosition(
                        Vector3(
                            x = control.userPosition.x,
                            y = control.userPosition.y +
                                if (control.dragging) 0f else 0.0075f * bob,
                            z = control.userPosition.z +
                                if (control.dragging) 0f else 0.004f * sin(bob) +
                                control.animatedLift,
                        ),
                    )
                    setEulerAngles(
                        EulerAngles(
                            pitch = -2.4f * bob,
                            yaw = 3.6f * bob,
                        ),
                    )
                    setScaleVector(
                        Vector3(
                            control.animatedScale,
                            control.animatedScale,
                            control.animatedScale,
                        ),
                    )
                }
            }
            delay(16)
        }
    }

    fun feelGrabbedCell(hand: FeelHand): Cell? =
        stateForFeelHand(hand)?.cell

    fun isCardSelectable(cell: Cell): Boolean =
        cards[cell]?.enabled == true

    fun isCardHeld(cell: Cell): Boolean =
        feelGrab?.cell == cell || secondFeelGrab?.cell == cell

    /**
     * Finds the nearest active card to a global pinch anchor. Used as a Feel-mode
     * fallback when a second simultaneous pinch is consumed by the board drag
     * recognizer and never reaches the tap detector.
     */
    fun nearestFeelCard(globalPosition: Vector3): Pair<Cell, Float>? {
        val root = boardRoot ?: return null
        val position = root.convertPositionFrom(globalPosition, null)
        var best: Pair<Cell, Float>? = null

        cards.forEach { (cell, card) ->
            if (!card.enabled) return@forEach
            val cardPosition = card.components[TransformComponent::class.java]?.position
                ?: return@forEach
            val distance = distance(position, cardPosition)
            if (best == null || distance < best!!.second) {
                best = cell to distance
            }
        }

        return best?.takeIf { it.second <= layout.cardSize * 2.25f }
    }

    private fun stateForFeelHand(hand: FeelHand): FeelGrabState? =
        if (hand == FeelHand.LEFT) feelGrab else secondFeelGrab

    fun beginFeelGrab(
        hand: FeelHand,
        cell: Cell,
    ): Boolean {
        if (stateForFeelHand(hand) != null) return false
        if (stateForFeelHand(hand.opposite())?.cell == cell) return false
        val card = cards[cell] ?: return false
        if (!card.enabled) return false
        feelReturnGenerations.remove(cell)

        val state = FeelGrabState(
            cell = cell,
            card = card,
            home = layout.gridPosition(cell.r, cell.c, cell.l),
        )
        if (hand == FeelHand.LEFT) {
            feelGrab = state
        } else {
            secondFeelGrab = state
        }
        setHighlight(cell, true)
        card.components[TransformComponent::class.java]?.apply {
            setPosition(state.current)
            setEulerAngles(EulerAngles())
            setScaleVector(Vector3(FEEL_GRAB_SCALE, FEEL_GRAB_SCALE, FEEL_GRAB_SCALE))
        }
        return true
    }

    /** Converts a global hand anchor into this rotated board's local space. */
    fun updateFeelHandTarget(
        hand: FeelHand,
        globalPosition: Vector3,
    ) {
        val state = stateForFeelHand(hand) ?: return
        val root = boardRoot ?: return
        state.releasing = false
        val target = root.convertPositionFrom(globalPosition, null)
        state.target = if (System.currentTimeMillis() < state.repelUntil) {
            target + state.repelOffset
        } else {
            target
        }
    }

    fun feelCardCollisionDistance(): Float = layout.cardSize * FEEL_CARD_COLLISION_FACTOR

    fun feelCardsDistance(): Float? {
        val first = feelGrab ?: return null
        val second = secondFeelGrab ?: return null
        return distance(first.current, second.current)
    }

    /**
     * Softly draws two held cards toward each other. This keeps the confirmation
     * gesture comfortable: hands remain apart, while the cards themselves meet.
     */
    fun attractFeelCards() {
        val first = feelGrab ?: return
        val second = secondFeelGrab ?: return
        val collisionDistance = feelCardCollisionDistance()
        val magnetDistance = layout.cardSize * FEEL_CARD_MAGNET_FACTOR
        val currentDistance = distance(first.current, second.current)
        val proximity = (
            1f - (currentDistance - collisionDistance) /
                (magnetDistance - collisionDistance)
            ).coerceIn(0f, 1f)
        first.proximity = proximity
        second.proximity = proximity

        if (currentDistance <= collisionDistance || currentDistance >= magnetDistance) return

        val direction = Vector3(
            x = second.current.x - first.current.x,
            y = second.current.y - first.current.y,
            z = second.current.z - first.current.z,
        )
        val directionLength = length(direction)
        if (directionLength < 0.0001f) return
        val unit = Vector3(
            x = direction.x / directionLength,
            y = direction.y / directionLength,
            z = direction.z / directionLength,
        )
        val pull = (magnetDistance - currentDistance) * FEEL_CARD_MAGNET_STRENGTH
        first.target = first.target + unit * pull
        second.target = second.target - unit * pull
    }

    fun updateEnergyLink() {
        val first = feelGrab
        val second = secondFeelGrab
        if (first == null || second == null) {
            energyLinkSegments.forEach { it.enabled = false }
            return
        }

        val collisionDistance = feelCardCollisionDistance()
        val magnetDistance = layout.cardSize * FEEL_CARD_MAGNET_FACTOR
        val currentDistance = distance(first.current, second.current)
        if (currentDistance > magnetDistance) {
            energyLinkSegments.forEach { it.enabled = false }
            return
        }

        val proximity = (
            1f - (currentDistance - collisionDistance) /
                (magnetDistance - collisionDistance)
            ).coerceIn(0f, 1f)
        val phase = (System.nanoTime() / 90_000_000f) % (2f * PI.toFloat())
        energyLinkSegments.forEachIndexed { index, segment ->
            val t = (index + 0.5f) / energyLinkSegments.size
            val position = Vector3(
                x = first.current.x + (second.current.x - first.current.x) * t,
                y = first.current.y + (second.current.y - first.current.y) * t +
                    sin(phase + t * 5.0f * PI.toFloat()) * 0.004f,
                z = first.current.z + (second.current.z - first.current.z) * t,
            )
            val edge = 1f - abs(t - 0.5f) * 2f
            val scale = (0.48f + 0.82f * proximity + 0.18f * edge) *
                (0.86f + 0.14f * sin(phase * 2f))
            segment.components[TransformComponent::class.java]?.apply {
                setPosition(position)
                setEulerAngles(
                    EulerAngles(
                        pitch = sin(phase + index) * 6f,
                        yaw = cos(phase + index) * 8f,
                    ),
                )
                setScaleVector(Vector3(scale, scale, scale))
            }
            segment.enabled = true
        }
    }

    /** Softly separates two invalid cards without dropping them from the hands. */
    fun bounceFeelCardsApart() {
        val first = feelGrab ?: return
        val second = secondFeelGrab ?: return
        val delta = Vector3(
            x = second.current.x - first.current.x,
            y = second.current.y - first.current.y,
            z = second.current.z - first.current.z,
        )
        val rawLength = length(delta)
        val direction = if (rawLength < 0.0001f) {
            Vector3(1f, 0.08f, 0f)
        } else {
            Vector3(delta.x / rawLength, delta.y / rawLength, delta.z / rawLength)
        }
        val until = System.currentTimeMillis() + 280L
        first.repelOffset = direction * -0.075f
        second.repelOffset = direction * 0.075f
        first.repelUntil = until
        second.repelUntil = until
        first.target = first.current + first.repelOffset
        second.target = second.current + second.repelOffset
    }

    fun playCardsContactImpact() {
        val first = feelGrab ?: return
        val second = secondFeelGrab ?: return
        playStarBurst(
            center = Vector3(
                x = (first.current.x + second.current.x) / 2f,
                y = (first.current.y + second.current.y) / 2f,
                z = (first.current.z + second.current.z) / 2f,
            ),
            count = 30,
            speed = 0.48f,
            life = 0.55f,
            spread = 0.10f,
        )
    }

    fun playSuccessStar(firstPosition: Vector3, secondPosition: Vector3) {
        val center = Vector3(
            x = (firstPosition.x + secondPosition.x) / 2f,
            y = (firstPosition.y + secondPosition.y) / 2f,
            z = (firstPosition.z + secondPosition.z) / 2f,
        )
        playStarBurst(
            center = center,
            count = 42,
            speed = 0.64f,
            life = 0.75f,
            spread = 0.055f,
        )
    }

    private fun playStarBurst(
        center: Vector3,
        count: Int,
        speed: Float,
        life: Float,
        spread: Float,
    ) {
        val random = Random(System.nanoTime())
        repeat(count) { index ->
            val theta = random.nextFloat() * 2f * PI.toFloat()
            val phi = acos(random.nextFloat() * 2f - 1f)
            val magnitude = speed * (0.55f + random.nextFloat() * 0.70f)
            val direction = Vector3(
                x = magnitude * sin(phi) * cos(theta),
                y = magnitude * cos(phi),
                z = magnitude * sin(phi) * sin(theta),
            )
            configureEffectParticle(
                origin = Vector3(
                    x = center.x + (random.nextFloat() - 0.5f) * spread,
                    y = center.y + (random.nextFloat() - 0.5f) * spread,
                    z = center.z + (random.nextFloat() - 0.5f) * spread,
                ),
                velocity = direction,
                life = life * (0.70f + random.nextFloat() * 0.55f),
                gravity = -0.18f,
                scale = 0.55f + random.nextFloat() * 0.85f,
                spin = 210f + random.nextFloat() * 500f,
                materialIndex = index,
            )
        }
    }

    /** Draws the solver path as a finite chain of glowing star dots. */
    fun playPath(path: List<Cell>, hint: Boolean = false) {
        if (path.size < 2) return
        val points = path.map { cell ->
            layout.gridPosition(cell.r, cell.c, cell.l)
        }
        val dotsPerSegment = if (hint) 5 else 7
        var materialBase = 0
        for (i in 0 until points.size - 1) {
            val from = points[i]
            val to = points[i + 1]
            for (step in 0 until dotsPerSegment) {
                val t = step / dotsPerSegment.toFloat()
                configureEffectParticle(
                    origin = Vector3(
                        x = from.x + (to.x - from.x) * t,
                        y = from.y + (to.y - from.y) * t,
                        z = from.z + (to.z - from.z) * t,
                    ),
                    velocity = Vector3(0f, 0.018f, 0f),
                    life = if (hint) 1.65f else 0.92f,
                    gravity = 0f,
                    scale = if (hint) 0.58f else 0.72f,
                    spin = 90f,
                    materialIndex = if (hint) 1 else materialBase,
                )
            }
            materialBase++
        }
    }

    fun playShuffleTrail(position: Vector3, index: Int) {
        val random = Random(System.nanoTime())
        configureEffectParticle(
            origin = Vector3(
                x = position.x + (random.nextFloat() - 0.5f) * 0.018f,
                y = position.y + (random.nextFloat() - 0.5f) * 0.018f,
                z = position.z + (random.nextFloat() - 0.5f) * 0.018f,
            ),
            velocity = Vector3(0f, -0.035f, 0f),
            life = 0.48f,
            gravity = -0.015f,
            scale = 0.34f + random.nextFloat() * 0.22f,
            spin = 160f,
            materialIndex = index,
        )
    }

    fun playCombo(
        count: Int,
        text: String,
    ) {
        val root = effectRoot ?: return
        val mesh = comboMesh ?: return
        if (count < 2) return
        val texture = createComboTexture(text).apply { toGlobal() }
        val material = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
            setBaseColorTexture(texture)
            setOpacity(0.96f)
            setDepthWrite(false)
            setApplyToneMapping(false)
            toGlobal()
        }
        val entity = ModelEntity(mesh, material).apply {
            setName("combo_notice_$count")
            components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        x = 0f,
                        y = layout.boardCenterY + layout.cardSize * 2.1f,
                        z = 0f,
                    ),
                )
                setEulerAngles(EulerAngles())
                setScaleVector(Vector3(0.001f, 0.001f, 0.001f))
            }
        }
        root.addChild(entity)

        effectScope.launch {
            val startAt = System.nanoTime()
            while (entity.valid) {
                val elapsed = ((System.nanoTime() - startAt) / 1_000_000f)
                if (elapsed >= 950f) break
                val t = (elapsed / 950f).coerceIn(0f, 1f)
                val pop = if (t < 0.18f) {
                    easeInOutCubic(t / 0.18f)
                } else {
                    1f - easeInOutCubic((t - 0.18f) / 0.82f) * 0.18f
                }
                val fade = (1f - ((t - 0.62f) / 0.38f).coerceIn(0f, 1f))
                entity.components[TransformComponent::class.java]?.apply {
                    setPosition(
                        Vector3(
                            x = 0f,
                            y = layout.boardCenterY +
                                layout.cardSize * (2.1f + t * 0.55f),
                            z = 0f,
                        ),
                    )
                    setScaleVector(
                        Vector3(
                            0.30f * pop,
                            0.115f * pop,
                            1f,
                        ),
                    )
                }
                material.setOpacity(0.96f * fade)
                delay(16)
            }
            entity.destroy()
            material.close()
            texture.close()
        }
    }

    private fun configureEffectParticle(
        origin: Vector3,
        velocity: Vector3,
        life: Float,
        gravity: Float,
        scale: Float,
        spin: Float,
        materialIndex: Int,
    ) {
        val particle = effectParticles.firstOrNull { !it.active } ?: return
        val materials = effectMaterials
        if (materials.isEmpty()) return
        particle.active = true
        particle.elapsed = 0f
        particle.life = life
        particle.origin = origin
        particle.velocity = velocity
        particle.gravity = gravity
        particle.scale = scale
        particle.spin = spin
        particle.entity.components[ModelComponent::class.java]?.materials?.set(
            index = 0,
            material = materials[materialIndex.mod(materials.size)],
        )
        particle.entity.enabled = true
        particle.entity.components[TransformComponent::class.java]?.apply {
            setPosition(origin)
            setEulerAngles(EulerAngles())
            setScaleVector(Vector3(scale, scale, scale))
        }
    }

    private fun updateEffectParticles(deltaTime: Float) {
        effectParticles.forEach { particle ->
            if (!particle.active) return@forEach
            particle.elapsed += deltaTime
            val age = particle.elapsed / particle.life
            if (age >= 1f) {
                particle.reset()
                return@forEach
            }
            val halfGravity = particle.gravity * 0.5f
            val position = Vector3(
                x = particle.origin.x + particle.velocity.x * particle.elapsed,
                y = particle.origin.y +
                    particle.velocity.y * particle.elapsed +
                    halfGravity * particle.elapsed * particle.elapsed,
                z = particle.origin.z + particle.velocity.z * particle.elapsed,
            )
            val fade = (1f - age).coerceIn(0f, 1f)
            val scale = particle.scale * (0.25f + 0.75f * fade)
            particle.entity.components[TransformComponent::class.java]?.apply {
                setPosition(position)
                setEulerAngles(
                    EulerAngles(
                        pitch = particle.elapsed * particle.spin,
                        yaw = particle.elapsed * particle.spin * 0.70f,
                    ),
                )
                setScaleVector(Vector3(scale, scale, scale))
            }
        }
    }

    fun releaseFeelHand(hand: FeelHand) {
        val state = stateForFeelHand(hand) ?: return
        // Detach first so the per-frame hand target loop can no longer overwrite
        // this card while it is flying home.
        if (hand == FeelHand.LEFT) {
            feelGrab = null
        } else {
            secondFeelGrab = null
        }
        setHighlight(state.cell, false)

        val generation = ++feelReturnGeneration
        feelReturnGenerations[state.cell] = generation
        effectScope.launch {
            val transform = state.card.components[TransformComponent::class.java]
                ?: return@launch
            val from = transform.position
            val startAt = System.nanoTime()

            while (
                feelReturnGenerations[state.cell] == generation &&
                    state.card.valid
            ) {
                val elapsed = ((System.nanoTime() - startAt) / 1_000_000f)
                    .coerceAtLeast(0f)
                    .coerceAtMost(220f)
                val t = elapsed / 220f
                val eased = 1f - (1f - t).let { it * it * it }
                transform.setPosition(lerp(from, state.home, eased))
                transform.setEulerAngles(EulerAngles())
                transform.setScaleVector(
                    Vector3(
                        1f + (FEEL_GRAB_SCALE - 1f) * (1f - eased),
                        1f + (FEEL_GRAB_SCALE - 1f) * (1f - eased),
                        1f + (FEEL_GRAB_SCALE - 1f) * (1f - eased),
                    ),
                )
                if (t >= 1f) break
                delay(16)
            }

            if (feelReturnGenerations[state.cell] == generation && state.card.valid) {
                transform.setPosition(state.home)
                transform.setEulerAngles(EulerAngles())
                transform.setScaleVector(Vector3(1f, 1f, 1f))
                feelReturnGenerations.remove(state.cell)
            }
        }
    }

    suspend fun runFeelGrabLoop(
        hand: FeelHand,
        cell: Cell,
    ) {
        while (stateForFeelHand(hand)?.cell == cell) {
            when (hand) {
                FeelHand.LEFT -> stepFeelGrab(1f / 60f)
                FeelHand.RIGHT -> stepFeelGrabState(
                    secondFeelGrab ?: return,
                    1f / 60f,
                )
            }
            delay(16)
        }
    }

    fun consumeFeelGrab(cell: Cell): ModelEntity? {
        val state = feelGrab ?: return null
        if (state.cell != cell) return null
        feelGrab = null
        clearFeelTargetHighlight()
        return state.card
    }

    fun consumeSecondFeelGrab(cell: Cell): ModelEntity? {
        val state = secondFeelGrab ?: return null
        if (state.cell != cell) return null
        secondFeelGrab = null
        return state.card
    }

    fun cancelFeelGrab() {
        val state = feelGrab ?: return
        state.card.components[TransformComponent::class.java]?.apply {
            setPosition(state.home)
            setEulerAngles(EulerAngles())
            setScaleVector(Vector3(1f, 1f, 1f))
        }
        feelGrab = null
        clearFeelTargetHighlight()
    }

    fun cancelSecondFeelGrab() {
        val state = secondFeelGrab ?: return
        state.card.components[TransformComponent::class.java]?.apply {
            setPosition(state.home)
            setEulerAngles(EulerAngles())
            setScaleVector(Vector3(1f, 1f, 1f))
        }
        setHighlight(state.cell, false)
        secondFeelGrab = null
    }

    suspend fun animateFeelReturn() {
        val state = feelGrab ?: return
        state.releasing = true
        state.target = state.home
        clearFeelTargetHighlight()

        while (true) {
            stepFeelGrab(1f / 60f)
            val dx = state.target.x - state.current.x
            val dy = state.target.y - state.current.y
            val dz = state.target.z - state.current.z
            val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (distance < 0.0025f && length(state.velocity) < 0.08f) {
                break
            }
            delay(16)
        }

        state.card.components[TransformComponent::class.java]?.apply {
            setPosition(state.home)
            setEulerAngles(EulerAngles())
            setScaleVector(Vector3(1f, 1f, 1f))
        }
        if (feelGrab === state) {
            feelGrab = null
        }
        updateFeelTargetHighlight(state)
    }

    /** Two grabbed cubes converge at their midpoint and vanish together. */
    suspend fun animateTwoHandMatchAndRemove(
        first: Cell,
        second: Cell,
        path: List<Cell>,
    ) {
        val firstCard = consumeFeelGrab(first) ?: cards[first] ?: return
        consumeSecondFeelGrab(second) ?: cards[second] ?: return
        clearFeelTargetHighlight()
        playPath(path)

        val firstTransform = firstCard.components[TransformComponent::class.java] ?: return
        val secondCard = cards[second] ?: return
        val secondTransform = secondCard.components[TransformComponent::class.java] ?: return
        val firstStart = firstTransform.position
        val secondStart = secondTransform.position
        val meet = Vector3(
            x = (firstStart.x + secondStart.x) / 2f,
            y = (firstStart.y + secondStart.y) / 2f,
            z = (firstStart.z + secondStart.z) / 2f + layout.cardSize * 0.25f,
        )
        val startAt = System.nanoTime()

        while (true) {
            val elapsed = ((System.nanoTime() - startAt) / 1_000_000f)
                .coerceAtLeast(0f)
                .coerceAtMost(TWO_HAND_MEET_DURATION_MS.toFloat())
            val t = elapsed / TWO_HAND_MEET_DURATION_MS
            val eased = easeInOutCubic(t)
            val scale = (1f - eased).coerceAtLeast(0.04f)

            firstTransform.setPosition(lerp(firstStart, meet, eased))
            secondTransform.setPosition(lerp(secondStart, meet, eased))
            firstTransform.setScaleVector(Vector3(scale, scale, scale))
            secondTransform.setScaleVector(Vector3(scale, scale, scale))

            if (t >= 1f) break
            delay(16)
        }

        firstCard.enabled = false
        secondCard.enabled = false
        playSuccessStar(firstStart, secondStart)
    }

    /** Invalid two-hand pairs knock together, then spring back home. */
    suspend fun animateTwoHandReject(
        first: Cell,
        second: Cell,
    ) {
        val firstCard = consumeFeelGrab(first) ?: cards[first] ?: return
        val secondCard = consumeSecondFeelGrab(second) ?: cards[second] ?: return
        clearFeelTargetHighlight()

        val firstTransform = firstCard.components[TransformComponent::class.java] ?: return
        val secondTransform = secondCard.components[TransformComponent::class.java] ?: return
        val firstStart = firstTransform.position
        val secondStart = secondTransform.position
        val firstHome = layout.gridPosition(first.r, first.c, first.l)
        val secondHome = layout.gridPosition(second.r, second.c, second.l)
        val meet = Vector3(
            x = (firstStart.x + secondStart.x) / 2f,
            y = (firstStart.y + secondStart.y) / 2f,
            z = (firstStart.z + secondStart.z) / 2f + layout.cardSize * 0.16f,
        )
        val startAt = System.nanoTime()

        while (true) {
            val elapsed = ((System.nanoTime() - startAt) / 1_000_000f)
                .coerceAtLeast(0f)
                .coerceAtMost(TWO_HAND_REJECT_DURATION_MS.toFloat())
            val t = elapsed / TWO_HAND_REJECT_DURATION_MS
            val returnT = easeInOutCubic(t)
            val knock = sin(PI * t).toFloat()
            val firstBase = lerp(firstStart, firstHome, returnT)
            val secondBase = lerp(secondStart, secondHome, returnT)
            val firstPull = 0.38f * knock
            val secondPull = 0.38f * knock
            val firstPosition = lerp(firstBase, meet, firstPull)
            val secondPosition = lerp(secondBase, meet, secondPull)
            val scale = 1f + 0.20f * knock

            firstTransform.setPosition(firstPosition)
            secondTransform.setPosition(secondPosition)
            firstTransform.setScaleVector(Vector3(scale, scale, scale))
            secondTransform.setScaleVector(Vector3(scale, scale, scale))

            if (t >= 1f) break
            delay(16)
        }

        listOf(first, second).forEach { cell ->
            cards[cell]?.components[TransformComponent::class.java]?.apply {
                setPosition(layout.gridPosition(cell.r, cell.c, cell.l))
                setEulerAngles(EulerAngles())
                setScaleVector(Vector3(1f, 1f, 1f))
            }
            setHighlight(cell, false)
        }
    }

    private fun lerp(
        from: Vector3,
        to: Vector3,
        t: Float,
    ): Vector3 = Vector3(
        x = from.x + (to.x - from.x) * t,
        y = from.y + (to.y - from.y) * t,
        z = from.z + (to.z - from.z) * t,
    )

    private fun nearestActiveCell(state: FeelGrabState): Cell? {
        var bestCell: Cell? = null
        var bestDistance = Float.MAX_VALUE

        cards.forEach { (cell, card) ->
            if (
                cell == state.cell ||
                    cell == feelGrab?.cell ||
                    cell == secondFeelGrab?.cell ||
                    !card.enabled
            ) {
                return@forEach
            }
            val position = layout.gridPosition(cell.r, cell.c, cell.l)
            val dx = position.x - state.current.x
            val dy = position.y - state.current.y
            val dz = position.z - state.current.z
            val distance = sqrt(dx * dx + dy * dy + dz * dz)
            if (distance < bestDistance) {
                bestDistance = distance
                bestCell = cell
            }
        }

        return bestCell
    }

    private fun updateFeelTargetHighlight(state: FeelGrabState) {
        val nearest = nearestActiveCell(state)
        if (nearest == null) {
            clearFeelTargetHighlight()
            return
        }

        val position = layout.gridPosition(nearest.r, nearest.c, nearest.l)
        val dx = position.x - state.current.x
        val dy = position.y - state.current.y
        val dz = position.z - state.current.z
        val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        val nextTarget =
            if (distance <= layout.cardSize * FEEL_TARGET_HIGHLIGHT_FACTOR) nearest else null

        if (nextTarget != feelTargetCell) {
            clearFeelTargetHighlight()
            if (nextTarget != null) {
                feelTargetCell = nextTarget
                setHighlight(nextTarget, true)
            }
        }
    }

    private fun clearFeelTargetHighlight() {
        feelTargetCell?.let { cell ->
            if (cell != feelGrab?.cell && cell != secondFeelGrab?.cell) {
                setHighlight(cell, false)
            }
        }
        feelTargetCell = null
    }

    private fun stepFeelGrab(deltaTime: Float) {
        val state = feelGrab ?: return
        stepFeelGrabState(state, deltaTime)
    }

    private fun stepFeelGrabState(
        state: FeelGrabState,
        deltaTime: Float,
    ) {
        val delta = state.target - state.current
        val acceleration = Vector3(
            x = delta.x * FEEL_SPRING - state.velocity.x * FEEL_DAMPING,
            y = delta.y * FEEL_SPRING - state.velocity.y * FEEL_DAMPING,
            z = delta.z * FEEL_SPRING - state.velocity.z * FEEL_DAMPING,
        )
        state.velocity = Vector3(
            x = (state.velocity.x + acceleration.x * deltaTime)
                .coerceIn(-FEEL_MAX_SPEED, FEEL_MAX_SPEED),
            y = (state.velocity.y + acceleration.y * deltaTime)
                .coerceIn(-FEEL_MAX_SPEED, FEEL_MAX_SPEED),
            z = (state.velocity.z + acceleration.z * deltaTime)
                .coerceIn(-FEEL_MAX_SPEED, FEEL_MAX_SPEED),
        )
        state.current += state.velocity * deltaTime

        val desiredScale = if (state.releasing) {
            1f
        } else {
            FEEL_GRAB_SCALE * (1f + 0.16f * state.proximity)
        }
        state.scale += (desiredScale - state.scale) * (deltaTime * 10f).coerceIn(0f, 1f)

        state.card.components[TransformComponent::class.java]?.apply {
            setPosition(state.current)
            setEulerAngles(
                EulerAngles(
                    pitch = (state.velocity.y * 5f).coerceIn(-9f, 9f),
                    yaw = (state.velocity.x * 5f).coerceIn(-9f, 9f),
                ),
            )
            setScaleVector(Vector3(state.scale, state.scale, state.scale))
        }
    }

    private fun length(value: Vector3): Float =
        kotlin.math.sqrt(value.x * value.x + value.y * value.y + value.z * value.z)

    /**
     * Moves every remaining cube to its shuffled destination along a staggered
     * 3D arc. Cards spin up during flight and settle back to unit orientation.
     */
    suspend fun animateShuffle(
        moves: Map<Cell, Cell>,
        onShuffleCardLand: () -> Unit,
    ) {
        if (moves.isEmpty()) return
        cancelFeelGrab()
        cancelSecondFeelGrab()

        val movingCards = moves.mapNotNull { (source, target) ->
            val card = cards[source] ?: return@mapNotNull null
            MovingCard(card, source, target)
        }

        movingCards.forEach { moving ->
            moving.card.enabled = true
            moving.card.components[TransformComponent::class.java]?.apply {
                setPosition(layout.gridPosition(moving.source.r, moving.source.c, moving.source.l))
                setEulerAngles(EulerAngles())
                setScaleVector(Vector3(1f, 1f, 1f))
            }
        }

        coroutineScope {
            movingCards.forEachIndexed { index, moving ->
                launch {
                    delay((index * SHUFFLE_STAGGER_MS).toLong())
                    animateCardFlight(moving, index) { shouldPlayLandingSound ->
                        if (shouldPlayLandingSound) onShuffleCardLand()
                    }
                }
            }
        }

        val reverseMoves = moves.entries.associate { it.value to it.key }
        val nextCards = LinkedHashMap<Cell, ModelEntity>()
        val nextKinds = HashMap<Cell, Int>()

        for (r in 0 until boardRows) {
            for (c in 0 until boardCols) {
                for (l in 0 until boardLayers) {
                    val cell = Cell(r, c, l)
                    val source = reverseMoves[cell]
                    if (source != null) {
                        nextCards[cell] =
                            requireNotNull(cards[source]) { "Missing source $source" }
                        nextKinds[cell] =
                            requireNotNull(cardKinds[source]) { "Missing kind $source" }
                    } else {
                        nextCards[cell] =
                            requireNotNull(cards[cell]) { "Missing inactive card $cell" }
                        nextKinds[cell] = cardKinds[cell] ?: LianliankanGame.EMPTY
                    }
                }
            }
        }

        cards.clear()
        cards.putAll(nextCards)
        cardKinds.clear()
        cardKinds.putAll(nextKinds)
    }

    private suspend fun animateCardFlight(
        moving: MovingCard,
        index: Int,
        onLand: (playSound: Boolean) -> Unit,
    ) {
        val transform = moving.card.components[TransformComponent::class.java] ?: return
        val from = layout.gridPosition(moving.source.r, moving.source.c, moving.source.l)
        val to = layout.gridPosition(moving.target.r, moving.target.c, moving.target.l)
        val distance = Vector3(to.x - from.x, to.y - from.y, to.z - from.z)
        val pathLength = (
            kotlin.math.abs(distance.x) + kotlin.math.abs(distance.y) +
                kotlin.math.abs(distance.z)
            ).coerceIn(0.75f, 2.4f)
        val startAt = System.nanoTime()

        while (true) {
            val elapsed = ((System.nanoTime() - startAt) / 1_000_000f)
                .coerceAtLeast(0f)
                .coerceAtMost(SHUFFLE_DURATION_MS.toFloat())
            val linear = elapsed / SHUFFLE_DURATION_MS
            val t = easeInOutCubic(linear)
            val arc = sin(PI * t).toFloat()
            val sideSwing = sin(2f * PI * t).toFloat()
            val flightScale = 1f + 0.52f * arc
            val position = Vector3(
                x = from.x + (to.x - from.x) * t,
                y = from.y + (to.y - from.y) * t +
                    layout.cardSize * (1.20f + pathLength * 0.48f) * arc,
                z = from.z + (to.z - from.z) * t +
                    sideSwing * layout.cardSize * 0.72f,
            )

            transform.setPosition(position)
            if (elapsed.mod(48f) < 16f) {
                playShuffleTrail(position, index)
            }
            transform.setEulerAngles(
                EulerAngles(
                    pitch = 720f * t,
                    yaw = 720f * t,
                ),
            )
            transform.setScaleVector(Vector3(flightScale, flightScale, flightScale))
            if (linear >= 1f) break
            delay(16)
        }

        transform.setPosition(to)
        transform.setEulerAngles(EulerAngles())
        transform.setScaleVector(Vector3(1f, 1f, 1f))
        onLand(index.mod(4) == 0)
    }

    private fun easeInOutCubic(t: Float): Float =
        if (t < 0.5f) {
            4f * t * t * t
        } else {
            1f - (-2f * t + 2f).let { it * it * it } / 2f
        }

    /**
     * Directly applies a two-axis orbit delta. Yaw wraps through 360 degrees;
     * pitch is clamped at straight top-down/bottom-up so the controls never
     * become inverted.
     */
    fun rotate(yawDeltaDegrees: Float, pitchDeltaDegrees: Float) {
        setBoardRotation(
            yawDegrees = boardYaw + yawDeltaDegrees,
            pitchDegrees = boardPitch + pitchDeltaDegrees,
        )
    }

    /**
     * Moves the complete board root in Stage space. This is deliberately
     * separate from orbit rotation: players can reposition a board that was
     * anchored behind them without changing which cube face they are viewing.
     */
    fun move(delta: Vector3) {
        val root = boardRoot ?: return
        val current = root.components[TransformComponent::class.java]?.position
            ?: boardPosition
        boardPosition = Vector3(
            x = (current.x + delta.x * BOARD_MOVE_GAIN).coerceIn(-2.60f, 2.60f),
            y = (current.y + delta.y * BOARD_MOVE_GAIN).coerceIn(0.35f, 2.60f),
            z = (current.z + delta.z * BOARD_MOVE_GAIN).coerceIn(-3.20f, 3.20f),
        )
        root.components[TransformComponent::class.java]?.setPosition(boardPosition)
    }

    fun resetPosition() {
        boardPosition = Vector3(
            x = 0f,
            y = STAGE_CONTENT_Y,
            z = STAGE_CONTENT_Z,
        )
        boardRoot?.components?.get(TransformComponent::class.java)?.setPosition(boardPosition)
    }

    fun boardPositionForLog(): Vector3 = boardPosition

    private fun setBoardRotation(yawDegrees: Float, pitchDegrees: Float) {
        val root = boardRoot ?: return
        boardYaw = (yawDegrees % 360f + 360f) % 360f
        boardPitch = pitchDegrees.coerceIn(-MAX_BOARD_PITCH, MAX_BOARD_PITCH)
        root.components[TransformComponent::class.java]?.apply {
            setEulerAngles(EulerAngles(pitch = boardPitch, yaw = boardYaw))
            setScaleVector(
                Vector3(
                    layout.rotationFitScale,
                    layout.rotationFitScale,
                    layout.rotationFitScale,
                ),
            )
        }
    }

    fun destroy() {
        val mesh = cardMesh
        val textures = cardTextures
        val materials = cardMaterials
        val controlMeshResource = controlMesh
        val controlTextureResources = controlTextures
        val controlMaterialResources = controlMaterials
        val softShadowMeshResource = softShadowMesh
        val softShadowTextureResource = softShadowTexture
        val softShadowMaterialResource = softShadowMaterial
        val celebrationMeshResource = celebrationMesh
        val cannonMeshResource = cannonMesh
        val celebrationMaterialResources = celebrationMaterials
        val cannonMaterialResources = cannonMaterials
        val effectMeshResource = effectMesh
        val effectMaterialResources = effectMaterials
        val energyLinkMaterialResource = energyLinkMaterial

        effectUpdateJob?.cancel()
        effectJob.cancel()

        boardRoot?.destroy()
        boardRoot = null
        celebrationRoot?.destroy()
        celebrationRoot = null
        cards.clear()
        cardKinds.clear()
        celebrationParticles.clear()
        effectParticles.clear()
        energyLinkSegments.clear()
        celebrationCannons.clear()
        floatingControls.values.forEach { control ->
            control.body.destroy()
            control.shadow.destroy()
        }
        floatingControls.clear()
        statusPanelBody?.destroy()
        statusPanelBody = null
        statusPanelShadow?.destroy()
        statusPanelShadow = null

        // These resources are deliberately global so they survive ModelComponent
        // replacement. Destroying the entity does not release them; release them
        // here after the hierarchy is gone.
        materials?.forEach { pair -> pair.forEach { it.close() } }
        textures?.forEach { it.close() }
        mesh?.close()
        controlMaterialResources?.values?.forEach { it.close() }
        controlTextureResources?.forEach { it.close() }
        controlMeshResource?.close()
        softShadowMaterialResource?.close()
        softShadowTextureResource?.close()
        softShadowMeshResource?.close()
        statusPanelMaterial?.close()
        statusPanelTexture?.close()
        statusPanelMesh?.close()
        celebrationMaterialResources.forEach { it.close() }
        cannonMaterialResources.forEach { it.close() }
        celebrationMeshResource?.close()
        cannonMeshResource?.close()

        cardMesh = null
        cardTextures = null
        cardMaterials = null
        controlMesh = null
        controlTextures = null
        controlMaterials = null
        softShadowMesh = null
        softShadowTexture = null
        softShadowMaterial = null
        statusPanelMesh = null
        statusPanelTexture = null
        statusPanelMaterial = null
        celebrationMesh = null
        cannonMesh = null
        celebrationMaterials = emptyList()
        cannonMaterials = emptyList()
        effectMesh?.close()
        comboMesh?.close()
        effectMaterials.forEach { it.close() }
        energyLinkMaterial?.close()
        effectRoot = null
        effectMesh = null
        comboMesh = null
        effectMaterials = emptyList()
        energyLinkMaterial = null
    }

    /** Brightens and scales the complete textured cube. */
    fun setHighlight(cell: Cell, selected: Boolean) {
        val card = cards[cell] ?: return
        val kind = cardKinds[cell] ?: return
        val cardMaterials = requireNotNull(cardMaterials) { "scene resources are released" }

        card.components[ModelComponent::class.java]?.materials?.set(
            index = 0,
            material = cardMaterials[kind][if (selected) 1 else 0],
        )
        // A grabbed card is animated by FeelGrabState; only its material should
        // change here so selection feedback cannot snap it back to its cell.
        if (feelGrab?.cell == cell || secondFeelGrab?.cell == cell) return
        card.components[TransformComponent::class.java]?.apply {
            val scale = if (selected) SELECTED_SCALE else 1f
            setScaleVector(Vector3(scale, scale, scale))
            val base = layout.gridPosition(cell.r, cell.c, cell.l)
            setPosition(
                Vector3(
                    base.x,
                    base.y,
                    base.z + if (selected) layout.cardSize * 0.72f else 0f,
                ),
            )
        }
    }

    suspend fun animateMatchAndRemove(cell: Cell) {
        val card = cards[cell] ?: return
        val transform = card.components[TransformComponent::class.java] ?: return
        val durationMs = 280
        val startAt = System.nanoTime()

        while (true) {
            val elapsed =
                ((System.nanoTime() - startAt) / 1_000_000f).coerceAtMost(durationMs.toFloat())
            val t = elapsed / durationMs
            val eased = t * t
            val scale = (1f - eased).coerceAtLeast(0.01f)
            transform.setScaleVector(Vector3(scale, scale, scale))
            if (t >= 1f) break
            delay(16)
        }

        card.enabled = false
    }

    /** Converges any matched pair into one star before removal. */
    suspend fun animatePairMatchAndRemove(
        first: Cell,
        second: Cell,
        path: List<Cell>,
    ) {
        val firstCard = cards[first] ?: return
        val secondCard = cards[second] ?: return
        val firstTransform = firstCard.components[TransformComponent::class.java] ?: return
        val secondTransform = secondCard.components[TransformComponent::class.java] ?: return
        val firstStart = firstTransform.position
        val secondStart = secondTransform.position
        val meet = Vector3(
            x = (firstStart.x + secondStart.x) / 2f,
            y = (firstStart.y + secondStart.y) / 2f,
            z = (firstStart.z + secondStart.z) / 2f + layout.cardSize * 0.30f,
        )
        val startAt = System.nanoTime()
        playPath(path)

        while (true) {
            val elapsed = ((System.nanoTime() - startAt) / 1_000_000f)
                .coerceAtLeast(0f)
                .coerceAtMost(TWO_HAND_MEET_DURATION_MS.toFloat())
            val t = elapsed / TWO_HAND_MEET_DURATION_MS
            val eased = easeInOutCubic(t)
            val scale = (1f - eased * 0.96f).coerceAtLeast(0.04f)
            firstTransform.setPosition(lerp(firstStart, meet, eased))
            secondTransform.setPosition(lerp(secondStart, meet, eased))
            firstTransform.setScaleVector(Vector3(scale, scale, scale))
            secondTransform.setScaleVector(Vector3(scale, scale, scale))
            if (t >= 1f) break
            delay(16)
        }

        firstCard.enabled = false
        secondCard.enabled = false
        playSuccessStar(firstStart, secondStart)
    }

    fun stopCelebration() {
        celebrationGeneration++
        celebrationRoot?.enabled = false
        celebrationParticles.forEach { particle ->
            particle.active = false
            particle.entity.enabled = false
        }
    }

    /**
     * Plays a finite, 7.6-second celebration sequence: three party cannons,
     * six spatial firework bursts, confetti with gravity, and cannon recoil.
     */
    suspend fun playCelebration(
        onPartyCannon: () -> Unit,
        onFirework: () -> Unit,
    ) {
        val root = celebrationRoot ?: return
        celebrationGeneration++
        val generation = celebrationGeneration
        celebrationParticles.forEach { it.reset() }
        root.enabled = true
        playConstellation()
        delay(1150L)

        val events = listOf(
            CelebrationEvent(
                atMillis = 0f,
                center = Vector3(0f, 0.230f, -0.030f),
                count = 46,
                minSpeed = 0.42f,
                maxSpeed = 0.70f,
                lifeSeconds = 1.55f,
                gravity = -0.30f,
            ),
            CelebrationEvent(
                atMillis = 1000f,
                center = Vector3(-0.170f, 0.190f, 0.070f),
                count = 38,
                minSpeed = 0.36f,
                maxSpeed = 0.62f,
                lifeSeconds = 1.40f,
                gravity = -0.30f,
            ),
            CelebrationEvent(
                atMillis = 2000f,
                center = Vector3(0.180f, 0.215f, -0.060f),
                count = 42,
                minSpeed = 0.38f,
                maxSpeed = 0.66f,
                lifeSeconds = 1.45f,
                gravity = -0.30f,
            ),
            CelebrationEvent(
                atMillis = 3100f,
                center = Vector3(0f, 0.270f, 0.090f),
                count = 48,
                minSpeed = 0.45f,
                maxSpeed = 0.75f,
                lifeSeconds = 1.60f,
                gravity = -0.28f,
            ),
            CelebrationEvent(
                atMillis = 4300f,
                center = Vector3(-0.210f, 0.170f, -0.080f),
                count = 36,
                minSpeed = 0.34f,
                maxSpeed = 0.60f,
                lifeSeconds = 1.35f,
                gravity = -0.31f,
            ),
            CelebrationEvent(
                atMillis = 5500f,
                center = Vector3(0.210f, 0.190f, 0.080f),
                count = 40,
                minSpeed = 0.38f,
                maxSpeed = 0.66f,
                lifeSeconds = 1.45f,
                gravity = -0.30f,
            ),
        )
        val startAt = System.nanoTime()
        var eventIndex = 0
        var lastCannonAt = -999f

        while (true) {
            if (celebrationGeneration != generation || !root.valid) break
            val elapsed = ((System.nanoTime() - startAt) / 1_000_000f)
                .coerceAtLeast(0f)
            if (elapsed >= CELEBRATION_DURATION_MS) break

            if (elapsed < 80f || (elapsed > 3600f && elapsed < 3680f)) {
                if (elapsed - lastCannonAt > 1000f) {
                    onPartyCannon()
                    lastCannonAt = elapsed
                    spawnConfetti()
                }
            }

            while (eventIndex < events.size && elapsed >= events[eventIndex].atMillis) {
                val event = events[eventIndex]
                spawnFireworkBurst(event)
                onFirework()
                eventIndex++
            }

            updateCelebrationParticles(CELEBRATION_FRAME_MS / 1000f)
            updateCannonRecoil(elapsed, lastCannonAt)
            delay(CELEBRATION_FRAME_MS.toLong())
        }

        if (celebrationGeneration == generation) {
            root.enabled = false
            celebrationParticles.forEach { it.reset() }
        }
    }

    /**
     * A short pre-firework constellation: the completed board momentarily
     * becomes connected starlight before the louder celebration begins.
     */
    private fun playConstellation() {
        val constellation = listOf(
            Vector3(0.000f, 0.300f, 0.020f) to Vector3(0.225f, 0.150f, -0.030f),
            Vector3(0.225f, 0.150f, -0.030f) to Vector3(0.150f, -0.105f, 0.050f),
            Vector3(0.150f, -0.105f, 0.050f) to Vector3(-0.150f, -0.105f, 0.050f),
            Vector3(-0.150f, -0.105f, 0.050f) to Vector3(-0.225f, 0.150f, -0.030f),
            Vector3(-0.225f, 0.150f, -0.030f) to Vector3(0.000f, 0.300f, 0.020f),
            Vector3(-0.225f, 0.150f, -0.030f) to Vector3(0.000f, 0.010f, 0.110f),
            Vector3(0.000f, 0.010f, 0.110f) to Vector3(0.225f, 0.150f, -0.030f),
        )

        constellation.forEachIndexed { lineIndex, (from, to) ->
            repeat(11) { step ->
                val particle = celebrationParticles.firstOrNull { !it.active } ?: return
                val t = step / 10f
                configureParticle(
                    particle = particle,
                    origin = Vector3(
                        x = from.x + (to.x - from.x) * t,
                        y = from.y + (to.y - from.y) * t,
                        z = from.z + (to.z - from.z) * t,
                    ),
                    velocity = Vector3(0f, 0.012f, 0f),
                    life = 1.75f,
                    gravity = 0f,
                    scale = if (step == 0 || step == 10) 1.15f else 0.62f,
                    spin = 100f,
                    material = requireNotNull(
                        celebrationMaterials[lineIndex % celebrationMaterials.size],
                    ),
                )
            }
        }
    }

    private fun spawnFireworkBurst(event: CelebrationEvent) {
        val materials = celebrationMaterials
        if (materials.isEmpty()) return
        val random = Random(20260918 + event.atMillis.toInt())
        var spawned = 0

        for (particle in celebrationParticles) {
            if (spawned >= event.count) break
            if (particle.active) continue

            val theta = random.nextFloat() * 2f * PI.toFloat()
            val phi = acos(random.nextFloat() * 2f - 1f)
            val speed = event.minSpeed +
                random.nextFloat() * (event.maxSpeed - event.minSpeed)
            val direction = Vector3(
                x = speed * sin(phi) * cos(theta),
                y = speed * cos(phi),
                z = speed * sin(phi) * sin(theta),
            )
            configureParticle(
                particle = particle,
                origin = event.center,
                velocity = direction,
                life = event.lifeSeconds,
                gravity = event.gravity,
                scale = 0.85f + random.nextFloat() * 0.60f,
                spin = 220f + random.nextFloat() * 420f,
                material = materials[(event.atMillis.toInt() + spawned) % materials.size],
            )
            spawned++
        }
    }

    private fun spawnConfetti() {
        val materials = celebrationMaterials
        if (materials.isEmpty()) return
        val random = Random(System.nanoTime())
        val origins = listOf(
            Vector3(-0.250f, -0.180f, 0.165f) to
                Vector3(0.58f, 0.78f, -0.04f),
            Vector3(0.250f, -0.180f, 0.165f) to
                Vector3(-0.58f, 0.78f, -0.04f),
            Vector3(0f, -0.215f, 0.195f) to
                Vector3(0f, 0.90f, -0.08f),
        )

        origins.forEachIndexed { cannonIndex, (origin, baseVelocity) ->
            repeat(24) { index ->
                val particle = celebrationParticles.firstOrNull { !it.active } ?: return
                val velocity = Vector3(
                    x = baseVelocity.x + random.nextFloat() * 0.30f - 0.15f,
                    y = baseVelocity.y + random.nextFloat() * 0.24f - 0.08f,
                    z = baseVelocity.z + random.nextFloat() * 0.24f - 0.12f,
                )
                configureParticle(
                    particle = particle,
                    origin = origin,
                    velocity = velocity,
                    life = 2.20f + random.nextFloat() * 0.70f,
                    gravity = -0.54f,
                    scale = 0.70f + random.nextFloat() * 0.75f,
                    spin = 360f + random.nextFloat() * 620f,
                    material = materials[(cannonIndex * 7 + index) % materials.size],
                )
            }
        }
    }

    private fun configureParticle(
        particle: CelebrationParticle,
        origin: Vector3,
        velocity: Vector3,
        life: Float,
        gravity: Float,
        scale: Float,
        spin: Float,
        material: UnlitMaterial,
    ) {
        particle.active = true
        particle.elapsed = 0f
        particle.life = life
        particle.origin = origin
        particle.velocity = velocity
        particle.gravity = gravity
        particle.scale = scale
        particle.spin = spin
        particle.entity.components[ModelComponent::class.java]?.materials?.set(
            index = 0,
            material = material,
        )
        particle.entity.enabled = true
        particle.entity.components[TransformComponent::class.java]?.apply {
            setPosition(origin)
            setEulerAngles(EulerAngles())
            setScaleVector(Vector3(scale, scale, scale))
        }
    }

    private fun updateCelebrationParticles(deltaTime: Float) {
        celebrationParticles.forEach { particle ->
            if (!particle.active) return@forEach
            particle.elapsed += deltaTime
            val age = particle.elapsed / particle.life
            if (age >= 1f) {
                particle.reset()
                return@forEach
            }

            val halfGravity = particle.gravity * 0.5f
            val position = Vector3(
                x = particle.origin.x + particle.velocity.x * particle.elapsed,
                y = particle.origin.y +
                    particle.velocity.y * particle.elapsed +
                    halfGravity * particle.elapsed * particle.elapsed,
                z = particle.origin.z + particle.velocity.z * particle.elapsed,
            )
            val fade = (1f - age).coerceIn(0f, 1f)
            val scale = particle.scale * (0.25f + 0.75f * fade)
            particle.entity.components[TransformComponent::class.java]?.apply {
                setPosition(position)
                setEulerAngles(
                    EulerAngles(
                        pitch = particle.elapsed * particle.spin,
                        yaw = particle.elapsed * particle.spin * 0.72f,
                    ),
                )
                setScaleVector(Vector3(scale, scale, scale))
            }
        }
    }

    private fun updateCannonRecoil(elapsed: Float, lastCannonAt: Float) {
        val age = ((elapsed - lastCannonAt) / 1000f).coerceAtLeast(0f)
        val recoil = if (age < 0.50f) {
            sin(PI * age / 0.50f).toFloat() * exp(-age * 5.0f)
        } else {
            0f
        }
        celebrationCannons.forEach { cannon ->
            cannon.entity.components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        x = cannon.basePosition.x,
                        y = cannon.basePosition.y - recoil * 0.016f,
                        z = cannon.basePosition.z - recoil * 0.010f,
                    ),
                )
                setEulerAngles(
                    EulerAngles(
                        pitch = cannon.basePitch + recoil * 10f,
                        yaw = cannon.baseYaw,
                    ),
                )
            }
        }
    }
}

@Composable
fun HomeVolume(
    appLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spatialNavigator = LocalSpatialNavigator.current
    val matchSound = remember { MatchSoundEffect(context) }
    val density = LocalDensity.current
    val physicalLengthConverter = LocalPhysicalLengthConverter.current
    val handControllerHaptic = LocalHandControllerHaptic.current
    val metersPerPixel =
        physicalLengthConverter.dpToLength(1.dp, LengthUnit.Meters) / density.density
    val comboLabel = stringResource(R.string.combo_label)
    val winVoiceText = stringResource(R.string.game_win_voice)
    // Feel is the current development mode; the floating control can return
    // the player to Casual without restarting the board.
    // Casual is the default because it works with gaze, controller, and the
    // emulator's GazePinch input. Feel mode remains one tap away.
    var mode by remember { mutableStateOf(GameMode.CASUAL) }
    var selectedDifficulty by remember { mutableStateOf(DifficultyChoice.PRESET_4) }
    var customRows by remember { mutableStateOf(3) }
    var customCols by remember { mutableStateOf(3) }
    var customLayers by remember { mutableStateOf(3) }
    var selectedSetupMode by remember { mutableStateOf(GameMode.CASUAL) }
    var activeBoardSpec by remember { mutableStateOf<BoardSpec?>(null) }
    var activeGameKey by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    val setupPanelPlacement = remember { SetupPanelPlacement() }
    var toolPanelYaw by remember { mutableStateOf(0f) }
    var toolPanelPitch by remember { mutableStateOf(0f) }

    // AttachmentPanel rendering is synchronized by Compose as well as ECS. Keep
    // both paths alive: the gesture updates ECS immediately, while this snapshot
    // flow lets the panel surface observe the same placement without passing the
    // position through the large setup-sheet content.
    LaunchedEffect(setupPanelPlacement) {
        snapshotFlow {
            listOf(
                setupPanelPlacement.position.x,
                setupPanelPlacement.position.y,
                setupPanelPlacement.position.z,
                setupPanelPlacement.yaw,
                setupPanelPlacement.pitch,
            )
        }.collect {
            setupPanelPlacement.apply()
        }
    }

    fun exitGame() {
        Log.i(INTERACTION_TAG, "exitGame action=closeStage")
        scope.launch {
            spatialNavigator.closeStage()
        }
    }

    fun rotateToolPanel(yawDeltaDegrees: Float, pitchDeltaDegrees: Float) {
        toolPanelYaw = (toolPanelYaw + yawDeltaDegrees) % 360f
        toolPanelPitch =
            (toolPanelPitch + pitchDeltaDegrees).coerceIn(-MAX_PANEL_PITCH, MAX_PANEL_PITCH)
    }

    fun toolPanelRotation() = EulerAngles(
        pitch = toolPanelPitch,
        yaw = toolPanelYaw,
    )

    fun bringSetupPanelToFront() {
        setupPanelPlacement.bringToFront()
    }

    if (activeBoardSpec == null) {
        InitialSetupVolume(
            appLanguage = appLanguage,
            onToggleLanguage = onToggleLanguage,
            selectedDifficulty = selectedDifficulty,
            onDifficultyChange = { selectedDifficulty = it },
            customRows = customRows,
            customCols = customCols,
            customLayers = customLayers,
            onCustomBoardChange = { rows, cols, layers ->
                customRows = rows
                customCols = cols
                customLayers = layers
            },
            selectedMode = selectedSetupMode,
            onModeChange = { selectedSetupMode = it },
            isInitialSetup = true,
            onStart = { spec ->
                mode = selectedSetupMode
                activeBoardSpec = spec
                activeGameKey++
                menuOpen = false
            },
            onClose = {},
            placement = setupPanelPlacement,
            onExit = ::exitGame,
        )
        return
    }

    val currentBoardSpec = activeBoardSpec ?: return

    key(activeGameKey) {

    val containerMeters =
        LocalPhysicalLengthConverter.current.dpToLength(960.dp, LengthUnit.Meters)
    val layout = remember(containerMeters, currentBoardSpec) {
        BoardLayout.fromContainerSize(
            containerMeters = containerMeters,
            rows = currentBoardSpec.rows,
            cols = currentBoardSpec.cols,
            layers = currentBoardSpec.layers,
        )
    }
    val game = remember(currentBoardSpec) {
        LianliankanGame(
            rows = currentBoardSpec.rows,
            cols = currentBoardSpec.cols,
            layers = currentBoardSpec.layers,
        )
    }
    val scene = remember(layout) { GameSceneState(layout) }
    val setupPanelScene = remember { SetupPanelSceneState() }
    val cosmosScene = remember(containerMeters) {
        CosmosBackgroundSceneState()
    }
    val handTrackingProvider = remember { HandTrackingProvider() }
    DisposableEffect(matchSound) {
        onDispose {
            matchSound.release()
        }
    }
    DisposableEffect(scene) {
        onDispose {
            scene.destroy()
        }
    }
    DisposableEffect(setupPanelScene) {
        onDispose {
            setupPanelScene.destroy()
        }
    }

    DisposableEffect(cosmosScene) {
        onDispose {
            cosmosScene.destroy()
        }
    }
    LaunchedEffect(cosmosScene) {
        while (true) {
            cosmosScene.update(0.033f)
            delay(33L)
        }
    }
    DisposableEffect(handTrackingProvider, mode) {
        if (mode == GameMode.FEEL) {
            val result = handTrackingProvider.start()
            Log.i(
                INTERACTION_TAG,
                "handTrackingStart result=$result " +
                    "support=${handTrackingProvider.supportState}",
            )
        }
        onDispose {
            if (mode == GameMode.FEEL) {
                handTrackingProvider.stop()
            }
        }
    }
    val setupPanelSize = panelSize(
        SETUP_PANEL_WIDTH,
        SETUP_PANEL_HEIGHT,
        LengthUnit.Meters,
    )
    val toolsPanelSize = panelSize(
        TOOL_PANEL_WIDTH,
        TOOL_PANEL_HEIGHT,
        LengthUnit.Meters,
    )
    val winPanelSize = panelSize(
        0.40f,
        0.155f,
        LengthUnit.Meters,
    )
    val failPanelSize = panelSize(
        0.32f,
        0.125f,
        LengthUnit.Meters,
    )
    val timedLimitMillis = remember(currentBoardSpec) {
        LianliankanGame.timedChallengeDurationMillis(currentBoardSpec.pairCount)
    }

    var cards by remember { mutableStateOf<Map<Cell, ModelEntity>>(emptyMap()) }
    var selectedCell by remember { mutableStateOf<Cell?>(null) }
    var cleared by remember { mutableStateOf(false) }
    var celebrationPlayed by remember { mutableStateOf(false) }
    var winActionsReady by remember { mutableStateOf(false) }
    var remainingTimeMillis by remember(timedLimitMillis) {
        mutableStateOf(timedLimitMillis)
    }
    var timedOut by remember { mutableStateOf(false) }
    var remainingPairs by remember { mutableStateOf(game.remainingPairs) }
    var comboCount by remember { mutableStateOf(0) }
    var lastMatchAtMillis by remember { mutableStateOf(0L) }
    var boardRevision by remember { mutableStateOf(0) }
    var boardRoot by remember { mutableStateOf<Entity?>(null) }
    var menuPanelEntity by remember { mutableStateOf<Entity?>(null) }
    var toolsPanelEntity by remember { mutableStateOf<Entity?>(null) }
    var winPanelEntity by remember { mutableStateOf<Entity?>(null) }
    var failPanelEntity by remember { mutableStateOf<Entity?>(null) }
    var hintsRemaining by remember { mutableStateOf(game.hintsRemaining) }
    var hintCells by remember { mutableStateOf<Set<Cell>>(emptySet()) }
    var isShuffling by remember { mutableStateOf(false) }
    var noticeRes by remember { mutableStateOf<Int?>(null) }
    val cellByEntity = remember(cards) {
        cards.entries.associate { (cell, entity) -> entity to cell }
    }
    val controlPreferences = remember {
        context.getSharedPreferences(
            "linklin_control_layout_cartoon_v9",
            android.content.Context.MODE_PRIVATE,
        )
    }
    val toolRailPreferences = remember {
        context.getSharedPreferences(
            "linklin_tool_rail_v1",
            android.content.Context.MODE_PRIVATE,
        )
    }
    var toolRailPosition by remember {
        mutableStateOf(
            Vector3(
                x = toolRailPreferences.getFloat("x", TOOL_RAIL_DEFAULT_POSITION.x),
                y = toolRailPreferences.getFloat("y", TOOL_RAIL_DEFAULT_POSITION.y),
                z = toolRailPreferences.getFloat("z", TOOL_RAIL_DEFAULT_POSITION.z),
            ),
        )
    }
    val savedControlPositions = remember(controlPreferences) {
        FLOATING_CONTROLS.associate { spec ->
            val x = controlPreferences.getFloat("${spec.id}_x", spec.position.x)
            val y = controlPreferences.getFloat("${spec.id}_y", spec.position.y)
            val z = controlPreferences.getFloat("${spec.id}_z", spec.position.z)
            spec.id to Vector3(x, y, z)
        }
    }

    fun persistControlPositions() {
        controlPreferences.edit().apply {
            scene.floatingControlPositions().forEach { (id, position) ->
                putFloat("${id}_x", position.x)
                putFloat("${id}_y", position.y)
                putFloat("${id}_z", position.z)
            }
        }.apply()
    }

    fun moveToolRail(delta: Vector3) {
        toolRailPosition = clampedToolRailPosition(toolRailPosition + delta)
        toolsPanelEntity?.components?.get(TransformComponent::class.java)?.setPosition(
            toolRailPosition,
        )
        toolRailPreferences.edit()
            .putFloat("x", toolRailPosition.x)
            .putFloat("y", toolRailPosition.y)
            .putFloat("z", toolRailPosition.z)
            .apply()
    }

    fun syncControlEnabledStates() {
        scene.setControlEnabled(
            id = "control_hint",
            enabled = hintsRemaining > 0 && !isShuffling && !cleared && !timedOut,
        )
        scene.setControlEnabled(
            id = "control_shuffle",
            enabled = !isShuffling && !cleared && !timedOut,
        )
        scene.setControlEnabled(
            id = "control_give_up",
            enabled = !isShuffling && !cleared && !timedOut,
        )
    }

    LaunchedEffect(hintsRemaining, isShuffling, cleared, timedOut) {
        syncControlEnabledStates()
    }

    LaunchedEffect(
        toolRailPosition,
        toolPanelYaw,
        toolPanelPitch,
        toolsPanelEntity,
    ) {
        toolsPanelEntity?.components?.get(TransformComponent::class.java)?.apply {
            setPosition(clampedToolRailPosition(toolRailPosition))
            setEulerAngles(toolPanelRotation())
        }
    }

    LaunchedEffect(
        menuOpen,
        menuPanelEntity,
        boardRoot,
        toolsPanelEntity,
        winPanelEntity,
        failPanelEntity,
        cleared,
        timedOut,
        mode,
    ) {
        scene.ensureFloatingControlsVisible()
        boardRoot?.enabled = !menuOpen && !cleared && !timedOut
        menuPanelEntity?.enabled = menuOpen
        setupPanelPlacement.apply()
        toolsPanelEntity?.enabled = !cleared && !timedOut && !menuOpen
        winPanelEntity?.enabled = cleared
        failPanelEntity?.enabled = mode == GameMode.TIMED && !cleared && !menuOpen
    }

    fun startCelebrationIfNeeded() {
        if (!game.isCleared || celebrationPlayed) return

        celebrationPlayed = true
        cleared = true
        winActionsReady = false
        matchSound.celebration(
            message = winVoiceText,
            language = appLanguage.locale,
        )
        scope.launch {
            scene.playCelebration(
                onPartyCannon = matchSound::partyCannon,
                onFirework = matchSound::firework,
            )
            // Keep the win card readable for a short beat after the final
            // firework, then let the player choose the next level or the menu.
            delay(700)
            winActionsReady = true
        }
    }

    fun syncSetupSelectionWithSpec(spec: BoardSpec) {
        selectedDifficulty = when (spec) {
            BoardSpec.EASY -> DifficultyChoice.PRESET_3
            BoardSpec.NORMAL -> DifficultyChoice.PRESET_4
            BoardSpec.HARD -> DifficultyChoice.PRESET_5
            else -> DifficultyChoice.CUSTOM
        }
        customRows = spec.rows
        customCols = spec.cols
        customLayers = spec.layers
        selectedSetupMode = mode
    }

    fun startNextLevel() {
        val next = currentBoardSpec.nextLevel
        syncSetupSelectionWithSpec(next)
        activeBoardSpec = next
        activeGameKey++
        menuOpen = false
    }

    fun returnToMenu() {
        syncSetupSelectionWithSpec(currentBoardSpec)
        activeBoardSpec = null
        menuOpen = false
    }

    fun restart() {
        game.restart()
        scene.stopCelebration()
        celebrationPlayed = false
        scene.reset(game)
        selectedCell = null
        cleared = false
        timedOut = false
        remainingTimeMillis = timedLimitMillis
        remainingPairs = game.remainingPairs
        hintsRemaining = game.hintsRemaining
        hintCells = emptySet()
        isShuffling = false
        noticeRes = null
        boardRevision++
    }

    fun clearHintHighlight() {
        hintCells.forEach { cell ->
            scene.setHighlight(cell, false)
        }
        hintCells = emptySet()
    }

    fun clearSelection() {
        val selected = selectedCell
        if (selected != null) {
            game.tap(selected)
            scene.setHighlight(selected, false)
            selectedCell = null
        }
    }

    fun retryTimedChallenge() {
        if (!timedOut) return
        restart()
    }

    LaunchedEffect(
        mode,
        activeGameKey,
        menuOpen,
        cleared,
        timedOut,
    ) {
        if (
            mode != GameMode.TIMED ||
                menuOpen ||
                cleared ||
                timedOut
        ) {
            return@LaunchedEffect
        }

        while (true) {
            delay(100L)
            val nextTime = (remainingTimeMillis - 100L).coerceAtLeast(0L)
            remainingTimeMillis = nextTime
            if (nextTime == 0L) {
                scene.cancelFeelGrab()
                scene.cancelSecondFeelGrab()
                clearSelection()
                clearHintHighlight()
                timedOut = true
                matchSound.failure()
                Log.i(
                    INTERACTION_TAG,
                    "timedChallengeOver remainingPairs=${game.remainingPairs}",
                )
                break
            }
        }
    }

    fun switchMode() {
        val nextMode = if (mode == GameMode.CASUAL) GameMode.FEEL else GameMode.CASUAL
        scene.cancelFeelGrab()
        scene.cancelSecondFeelGrab()
        clearSelection()
        clearHintHighlight()
        mode = nextMode
        noticeRes = when (nextMode) {
            GameMode.CASUAL -> R.string.mode_casual_gaze
            GameMode.FEEL -> R.string.mode_feel_active
            GameMode.TIMED -> R.string.mode_timed_active
        }
    }

    fun playFeelHaptic(
        level: Int,
        frequency: Int,
        duration: Int,
        deviceX: Float,
    ) {
        val hand = if (deviceX == 0f) {
            HandController.Unknown
        } else if (deviceX < 0f) {
            HandController.Left
        } else {
            HandController.Right
        }
        handControllerHaptic.feedback(
            type = HandControllerHapticType.custom(
                level = level,
                frequency = frequency,
                duration = duration,
            ),
            handController = hand,
        )
    }

    fun playTwoHandHaptic(
        level: Int,
        frequency: Int,
        duration: Int,
        firstDeviceX: Float,
        secondDeviceX: Float,
    ) {
        playFeelHaptic(level, frequency, duration, firstDeviceX)
        playFeelHaptic(level, frequency, duration, secondDeviceX)
    }

    fun registerComboMatch(): Int {
        val now = System.currentTimeMillis()
        comboCount = if (now - lastMatchAtMillis <= 4_000L) comboCount + 1 else 1
        lastMatchAtMillis = now
        return comboCount
    }

    fun resetCombo() {
        comboCount = 0
        lastMatchAtMillis = 0L
    }

    fun requestHint() {
        if (cleared || timedOut || isShuffling) return

        clearSelection()
        clearHintHighlight()
        val pair = game.requestHint()
        if (pair == null) {
            noticeRes = if (game.hintsRemaining <= 0) {
                R.string.hint_none_left
            } else {
                R.string.hint_no_move
            }
            return
        }

        scene.setHighlight(pair.first, true)
        scene.setHighlight(pair.second, true)
        game.pathFor(pair.first, pair.second)?.let { path ->
            scene.playPath(path, hint = true)
        }
        hintCells = setOf(pair.first, pair.second)
        hintsRemaining = game.hintsRemaining
        noticeRes = null
    }

    fun shuffleBoard() {
        if (cleared || timedOut || isShuffling) return

        scope.launch {
            isShuffling = true
            noticeRes = R.string.shuffle_in_progress
            clearSelection()
            clearHintHighlight()

            val moves = withContext(Dispatchers.Default) {
                game.shuffleRemaining()
            }
            if (moves.isEmpty()) {
                noticeRes = R.string.shuffle_no_move
                isShuffling = false
                return@launch
            }

            matchSound.shuffleWhoosh()
            scene.animateShuffle(moves) {
                matchSound.cardLand()
            }
            cards = scene.cards.toMap()
            selectedCell = null
            hintCells = emptySet()
            remainingPairs = game.remainingPairs
            noticeRes = null
            boardRevision++
            isShuffling = false
        }
    }

    fun onFloatingControlTap(id: String?) {
        when (id) {
            "control_hint" -> requestHint()
            "control_shuffle" -> shuffleBoard()
            "control_give_up" -> restart()
        }
    }

    fun chooseFeelHandForTap(
        tapPosition: Offset3D,
    ): FeelHand {
        val data = handTrackingProvider.latestData
        val left = handAnchor(data, FeelHand.LEFT)
        val right = handAnchor(data, FeelHand.RIGHT)

        val leftPinch = data.leftPinch?.takeIf { it.isReady }?.pinchValue ?: 0f
        val rightPinch = data.rightPinch?.takeIf { it.isReady }?.pinchValue ?: 0f
        val leftAim = data.leftAim?.takeIf { it.isReady }?.aimValue ?: 0f
        val rightAim = data.rightAim?.takeIf { it.isReady }?.aimValue ?: 0f
        val leftScore = leftPinch * 1.45f + leftAim
        val rightScore = rightPinch * 1.45f + rightAim

        val occupiedLeft = scene.feelGrabbedCell(FeelHand.LEFT) != null
        val occupiedRight = scene.feelGrabbedCell(FeelHand.RIGHT) != null

        // GazePinch and some DirectPinch events do not carry a hand identity.
        // Spatial-view inputDevicePose and HandTracking anchors use different
        // spaces/units, so never compare them directly. Occupancy plus real
        // pinch strength is the reliable order on hardware.
        val freeHand = when {
            occupiedLeft && !occupiedRight -> FeelHand.RIGHT
            occupiedRight && !occupiedLeft -> FeelHand.LEFT
            else -> null
        }
        val freeHandPinch = when (freeHand) {
            FeelHand.LEFT -> leftPinch
            FeelHand.RIGHT -> rightPinch
            null -> 0f
        }
        val hand = when {
            // A held hand can remain strongly pinched. If the other hand is
            // actively pinching, an unheld card belongs to that free hand.
            freeHand != null && freeHandPinch > 0.30f -> freeHand

            maxOf(leftScore, rightScore) > 0.18f &&
                abs(leftScore - rightScore) > 0.04f ->
                if (leftScore > rightScore) FeelHand.LEFT else FeelHand.RIGHT

            // If only one hand is visible, it must own the tap.
            left != null && right == null -> FeelHand.LEFT
            right != null && left == null -> FeelHand.RIGHT

            // A fuzzy second card must go to the free hand. Without this
            // fallback, ambiguous taps repeatedly replace the right-hand card.
            occupiedLeft != occupiedRight -> if (occupiedLeft) FeelHand.RIGHT else FeelHand.LEFT

            leftScore >= rightScore -> FeelHand.LEFT
            else -> FeelHand.RIGHT
        }

        Log.i(
            INTERACTION_TAG,
            "feelHandSignal hand=$hand " +
                "leftPinch=$leftPinch rightPinch=$rightPinch " +
                "leftAim=$leftAim rightAim=$rightAim " +
                "leftVisible=${left != null} rightVisible=${right != null} " +
                "occupiedLeft=$occupiedLeft occupiedRight=$occupiedRight",
        )
        return hand
    }

    val feelTapGuards = mutableMapOf<FeelHand, Cell>()
    val feelSelectionTimes = mutableMapOf<FeelHand, Long>()

    fun assignFeelCard(
        hand: FeelHand,
        cell: Cell,
    ): Boolean {
        if (scene.feelGrabbedCell(hand.opposite()) == cell) return false
        if (scene.feelGrabbedCell(hand) == cell) return false
        if (scene.feelGrabbedCell(hand) != null) {
            scene.releaseFeelHand(hand)
        }
        if (!scene.isCardSelectable(cell) || !scene.beginFeelGrab(hand, cell)) {
            return false
        }

        selectedCell = null
        feelSelectionTimes[hand] = System.currentTimeMillis()
        matchSound.cardSelect()
        scope.launch {
            scene.runFeelGrabLoop(hand, cell)
        }
        playFeelHaptic(
            level = 48,
            frequency = 78,
            duration = 9,
            deviceX = if (hand == FeelHand.LEFT) -1f else 1f,
        )
        return true
    }

    fun onFeelCardTap(
        cell: Cell,
        tapPosition: Offset3D,
    ) {
        if (cleared || timedOut || isShuffling || mode != GameMode.FEEL) return

        clearHintHighlight()
        val hand = chooseFeelHandForTap(tapPosition)
        val current = scene.feelGrabbedCell(hand)
        val otherHandCell = scene.feelGrabbedCell(hand.opposite())
        val guardedCell = feelTapGuards[hand]

        // A proximity fallback may already have assigned this cell at pinch onset.
        // Suppress the duplicate tap/up event so it does not immediately release
        // the card that the player just picked up.
        if (guardedCell == cell) {
            feelTapGuards.remove(hand)
            Log.i(INTERACTION_TAG, "feelTapGuardSuppressed hand=$hand cell=$cell")
            return
        }

        Log.i(
            INTERACTION_TAG,
            "feelCardTap hand=$hand cell=$cell current=$current " +
                "other=$otherHandCell tap=${tapPosition.x},${tapPosition.y}," +
                "${tapPosition.z}",
        )

        // A quick pinch is an idempotent selection. Repeated spatial events for
        // the same physical pinch must not toggle the card off; a deliberate
        // hand flick is the release gesture.
        if (otherHandCell == cell) return
        if (current == cell) {
            Log.i(
                INTERACTION_TAG,
                "feelSameCardTapIgnored hand=$hand cell=$cell",
            )
            return
        }
        assignFeelCard(hand, cell)
    }

    fun onFeelCardsTouch(
        first: Cell,
        second: Cell,
        firstDeviceX: Float,
        secondDeviceX: Float,
    ) {
        if (
            cleared ||
                timedOut ||
                isShuffling ||
                mode != GameMode.FEEL ||
                scene.feelGrabbedCell(FeelHand.LEFT) != first ||
                scene.feelGrabbedCell(FeelHand.RIGHT) != second
        ) {
            return
        }

        when (val result = game.match(first, second)) {
            is MoveResult.Matched -> {
                selectedCell = null
                scene.playCardsContactImpact()
                matchSound.cardContact()
                scope.launch {
                    scene.animateTwoHandMatchAndRemove(result.a, result.b, result.path)
                    startCelebrationIfNeeded()
                }
                val combo = registerComboMatch()
                scene.playCombo(
                    count = combo,
                    text = "$comboLabel ×$combo",
                )
                playTwoHandHaptic(
                    level = 95,
                    frequency = 150,
                    duration = 42,
                    firstDeviceX = firstDeviceX,
                    secondDeviceX = secondDeviceX,
                )
                matchSound.success(combo)
            }

            is MoveResult.Selected,
            is MoveResult.Deselected,
            MoveResult.NotMatched,
            -> {
                selectedCell = null
                scene.playCardsContactImpact()
                scene.bounceFeelCardsApart()
                matchSound.cardContact()
                playTwoHandHaptic(
                    level = 60,
                    frequency = 66,
                    duration = 22,
                    firstDeviceX = firstDeviceX,
                    secondDeviceX = secondDeviceX,
                )
                matchSound.failure()
                resetCombo()
                Log.i(
                    INTERACTION_TAG,
                    "feelCardsTouchRejected first=$first second=$second; " +
                        "tap a replacement card with either hand",
                )
            }
        }
        remainingPairs = game.remainingPairs
    }

    LaunchedEffect(mode, boardRoot, handTrackingProvider) {
        if (mode != GameMode.FEEL) return@LaunchedEffect

        val flickLastPosition = mutableMapOf<FeelHand, Vector3>()
        val flickLastNanos = mutableMapOf<FeelHand, Long>()
        val flickSmoothedSpeed = mutableMapOf<FeelHand, Float>()
        val flickFastFrames = mutableMapOf<FeelHand, Int>()

        fun resetFlickMotion(hand: FeelHand) {
            flickLastPosition.remove(hand)
            flickLastNanos.remove(hand)
            flickSmoothedSpeed[hand] = 0f
            flickFastFrames[hand] = 0
        }

        fun handleFeelFlick(
            hand: FeelHand,
            anchor: Vector3,
        ) {
            if (scene.feelGrabbedCell(hand) == null) {
                resetFlickMotion(hand)
                return
            }

            val lastPosition = flickLastPosition[hand]
            val lastNanos = flickLastNanos[hand]
            if (lastPosition == null || lastNanos == null) {
                flickLastPosition[hand] = anchor
                flickLastNanos[hand] = System.nanoTime()
                flickSmoothedSpeed[hand] = 0f
                flickFastFrames[hand] = 0
                return
            }

            val nowNanos = System.nanoTime()
            val deltaTimeSeconds = (nowNanos - lastNanos) / 1_000_000_000f
            if (deltaTimeSeconds <= 0f || deltaTimeSeconds > 0.080f) {
                flickLastPosition[hand] = anchor
                flickLastNanos[hand] = nowNanos
                flickFastFrames[hand] = 0
                return
            }

            val instantSpeed = distance(anchor, lastPosition) / deltaTimeSeconds
            val smoothedSpeed = flickSmoothedSpeed[hand] ?: 0f
            val nextSmoothedSpeed =
                smoothedSpeed * 0.62f + instantSpeed * 0.38f
            // Only sustained real motion can release a wrong card. Acceleration
            // alone produced false releases while the hand was almost stationary.
            val isFastFrame =
                instantSpeed >= FEEL_FLICK_SPEED_METERS_PER_SECOND
            val nextFastFrames =
                if (isFastFrame) {
                    (flickFastFrames[hand] ?: 0) + 1
                } else {
                    0
                }

            flickLastPosition[hand] = anchor
            flickLastNanos[hand] = nowNanos
            flickSmoothedSpeed[hand] = nextSmoothedSpeed
            flickFastFrames[hand] = nextFastFrames

            val selectedAt = feelSelectionTimes[hand] ?: 0L
            val selectionAgeMillis = System.currentTimeMillis() - selectedAt
            val deliberateFlick = nextFastFrames >= FEEL_FLICK_FAST_FRAMES &&
                nextSmoothedSpeed >= FEEL_FLICK_SMOOTHED_SPEED

            if (
                deliberateFlick &&
                    selectionAgeMillis >= FEEL_FLICK_SELECT_COOLDOWN_MS
            ) {
                val releasedCell = scene.feelGrabbedCell(hand)
                scene.releaseFeelHand(hand)
                resetFlickMotion(hand)
                Log.i(
                    INTERACTION_TAG,
                    "feelFlickRelease hand=$hand cell=$releasedCell " +
                        "speed=$nextSmoothedSpeed",
                )
            }
        }

        fun handleFeelPinchOnset(
            hand: FeelHand,
            anchor: Vector3,
        ) {
            if (cleared || timedOut || isShuffling) return
            val nearest = scene.nearestFeelCard(anchor)
            if (nearest == null) return

            val cell = nearest.first
            val heldByThisHand = scene.feelGrabbedCell(hand)
            val heldByOtherHand = scene.feelGrabbedCell(hand.opposite())

            // A guard exists when the previous proximity selection's tap/up event
            // never arrived. A new pinch is therefore the next intentional press.
            if (feelTapGuards[hand] != null) {
                feelTapGuards.remove(hand)
            }

            if (heldByThisHand == cell) {
                // Repeated pinch events for the same held card are ignored.
                // A deliberate flick releases the card instead.
                Log.i(
                    INTERACTION_TAG,
                    "feelSameCardPinchIgnored hand=$hand cell=$cell",
                )
                return
            }
            if (heldByOtherHand == cell) return

            if (assignFeelCard(hand, cell)) {
                feelTapGuards[hand] = cell
                Log.i(
                    INTERACTION_TAG,
                    "feelProximitySelect hand=$hand cell=$cell " +
                        "distance=${nearest.second}",
                )
            }
        }

        var cardsTouchArmed = true
        var loggedTracking = false
        var previousPair: Pair<Cell, Cell>? = null
        var leftPinchWasActive = false
        var rightPinchWasActive = false
        while (true) {
            val root = boardRoot
            val data = handTrackingProvider.latestData
            val left = handAnchor(data, FeelHand.LEFT)
            val right = handAnchor(data, FeelHand.RIGHT)
            if (root != null && left != null) {
                handleFeelFlick(FeelHand.LEFT, left)
            } else {
                resetFlickMotion(FeelHand.LEFT)
            }
            if (root != null && right != null) {
                handleFeelFlick(FeelHand.RIGHT, right)
            } else {
                resetFlickMotion(FeelHand.RIGHT)
            }
            val leftPinchValue =
                data.leftPinch?.takeIf { it.isReady }?.pinchValue ?: 0f
            val rightPinchValue =
                data.rightPinch?.takeIf { it.isReady }?.pinchValue ?: 0f
            val leftPinchIsActive = leftPinchValue > 0.45f
            val rightPinchIsActive = rightPinchValue > 0.45f
            val leftPinchOnset = leftPinchIsActive && !leftPinchWasActive
            val rightPinchOnset = rightPinchIsActive && !rightPinchWasActive

            if (root != null && leftPinchOnset && left != null) {
                handleFeelPinchOnset(FeelHand.LEFT, left)
            }
            if (root != null && rightPinchOnset && right != null) {
                handleFeelPinchOnset(FeelHand.RIGHT, right)
            }

            leftPinchWasActive = leftPinchValue > 0.22f
            rightPinchWasActive = rightPinchValue > 0.22f

            if (root != null && left != null) {
                scene.updateFeelHandTarget(FeelHand.LEFT, left)
            }
            if (root != null && right != null) {
                scene.updateFeelHandTarget(FeelHand.RIGHT, right)
            }
            if (root != null && (left != null || right != null)) {
                scene.attractFeelCards()
            }

            if (root != null && left != null && right != null) {
                if (!loggedTracking) {
                    loggedTracking = true
                    Log.i(
                        INTERACTION_TAG,
                        "handTrackingReady left=${left.x},${left.y},${left.z} " +
                            "right=${right.x},${right.y},${right.z}",
                    )
                }

                val first = scene.feelGrabbedCell(FeelHand.LEFT)
                val second = scene.feelGrabbedCell(FeelHand.RIGHT)
                val cardDistance = scene.feelCardsDistance()
                val currentPair = if (first != null && second != null) first to second else null
                if (currentPair != previousPair) {
                    cardsTouchArmed = true
                    previousPair = currentPair
                }
                val contactDistance = scene.feelCardCollisionDistance()
                if (cardDistance != null && cardDistance > contactDistance * 1.55f) {
                    cardsTouchArmed = true
                }

                if (
                    cardsTouchArmed &&
                        cardDistance != null &&
                        cardDistance <= contactDistance &&
                        first != null &&
                        second != null
                ) {
                    cardsTouchArmed = false
                    Log.i(
                        INTERACTION_TAG,
                        "feelCardsTouch first=$first second=$second " +
                            "cardDistance=$cardDistance threshold=$contactDistance",
                    )
                    onFeelCardsTouch(
                        first = first,
                        second = second,
                        firstDeviceX = left.x,
                        secondDeviceX = right.x,
                    )
                }
            } else {
                cardsTouchArmed = true
            }

            delay(FEEL_HAND_TRACK_FRAME_MS)
        }
    }

    fun onCardTap(cell: Cell) {
        if (cleared || timedOut || isShuffling) return

        if (hintCells.isNotEmpty()) {
            clearHintHighlight()
        }

        val previous = selectedCell
        val result = game.tap(cell)
        when (result) {
            is MoveResult.Selected -> {
                scene.setHighlight(cell, true)
                selectedCell = cell
                matchSound.cardSelect()
            }

            is MoveResult.Deselected -> {
                scene.setHighlight(cell, false)
                selectedCell = null
            }

            is MoveResult.Matched -> {
                scene.setHighlight(result.a, false)
                scene.setHighlight(result.b, false)
                selectedCell = null
                scene.playPath(result.path)
                scope.launch {
                    scene.animatePairMatchAndRemove(result.a, result.b, result.path)
                    startCelebrationIfNeeded()
                }
                val combo = registerComboMatch()
                scene.playCombo(
                    count = combo,
                    text = "$comboLabel ×$combo",
                )
                matchSound.success(combo)
            }

            MoveResult.NotMatched -> {
                previous?.let { scene.setHighlight(it, false) }
                selectedCell = null
                matchSound.failure()
                resetCombo()
            }
        }
        remainingPairs = game.remainingPairs
    }

    var tapModifier: Modifier = Modifier
    if (!menuOpen) boardRoot?.let { root ->
        // Keep board behavior stable and predictable: direct hand pinches move
        // the board, while ray/gaze pinches orbit it. Panel orientation is now
        // handled by the setup/tool AttachmentPanels instead.
        tapModifier = tapModifier.pointerInput(root, mode) {
            var feelCardDragGesture = false
            var feelCardDragDistance = 0f
            var feelHoldRotateStartedAt = 0L
            var feelHoldRotateActive = false
            detectSpatialDragGesture(
                context = context,
                targetedToEntity = TargetEntity.hit(root),
                onDragStart = { location, dragValue ->
                    val dragCell = cellByEntity[dragValue.targetEntity]
                    feelCardDragGesture = mode == GameMode.FEEL && dragCell != null
                    feelCardDragDistance = 0f
                    feelHoldRotateStartedAt = System.currentTimeMillis()
                    feelHoldRotateActive = false
                    if (
                        mode == GameMode.FEEL &&
                            dragCell != null &&
                            scene.isCardSelectable(dragCell) &&
                            !scene.isCardHeld(dragCell)
                    ) {
                        Log.i(
                            INTERACTION_TAG,
                            "feelDragSelect cell=$dragCell " +
                                "kind=${dragValue.interactionKind}",
                        )
                        onFeelCardTap(
                            cell = dragCell,
                            tapPosition = location,
                        )
                    }
                    Log.i(
                        INTERACTION_TAG,
                        "spatialDragStart mode=$mode " +
                            "kind=${dragValue.interactionKind} " +
                            "action=" +
                                if (feelCardDragGesture) {
                                    "feelCard"
                                } else if (
                                    dragValue.interactionKind == InteractionKind.DirectPinch
                                ) {
                                    "move"
                                } else {
                                    "rotate"
                                },
                    )
                },
                onDrag = { dragValue ->
                    if (feelCardDragGesture) {
                        feelCardDragDistance += sqrt(
                            square(dragValue.dragAmount.x) +
                                square(dragValue.dragAmount.y) +
                                square(dragValue.dragAmount.z),
                        )
                        val bothCardsHeld =
                            scene.feelGrabbedCell(FeelHand.LEFT) != null &&
                                scene.feelGrabbedCell(FeelHand.RIGHT) != null

                        // Two held cards are in the matching gesture. Keep that
                        // behavior unchanged and never add board orbiting to it.
                        if (bothCardsHeld) {
                            if (feelHoldRotateActive) {
                                Log.i(
                                    INTERACTION_TAG,
                                    "feelHoldRotatePause reason=bothCardsHeld",
                                )
                            }
                            feelHoldRotateActive = false
                            return@detectSpatialDragGesture
                        }

                        val holdElapsedMs =
                            System.currentTimeMillis() - feelHoldRotateStartedAt
                        if (
                            !feelHoldRotateActive &&
                                holdElapsedMs >= FEEL_HOLD_BOARD_ROTATION_MS
                        ) {
                            feelHoldRotateActive = true
                            Log.i(
                                INTERACTION_TAG,
                                "feelHoldRotateStart elapsedMs=$holdElapsedMs " +
                                    "distance=$feelCardDragDistance",
                            )
                        }

                        // A short pinch still only selects/stickies a card. Once
                        // the same pinch is deliberately held, hand movement
                        // becomes a two-axis orbit of the game body. The card
                        // continues following its hand anchor.
                        if (feelHoldRotateActive) {
                            scene.rotate(
                                yawDeltaDegrees = dragValue.dragAmount.x *
                                    YAW_DEGREES_PER_PIXEL,
                                pitchDeltaDegrees = -dragValue.dragAmount.y *
                                    PITCH_DEGREES_PER_PIXEL,
                            )
                        }
                        return@detectSpatialDragGesture
                    } else if (dragValue.interactionKind == InteractionKind.DirectPinch) {
                        scene.move(
                            Vector3(
                                x = dragValue.dragAmount.x * metersPerPixel,
                                y = -dragValue.dragAmount.y * metersPerPixel,
                                z = dragValue.dragAmount.z * metersPerPixel,
                            ),
                        )
                    } else {
                        scene.rotate(
                            yawDeltaDegrees = dragValue.dragAmount.x * YAW_DEGREES_PER_PIXEL,
                            pitchDeltaDegrees = -dragValue.dragAmount.y *
                                PITCH_DEGREES_PER_PIXEL,
                        )
                    }
                },
                onDragEnd = { dragValue ->
                    if (
                        !feelCardDragGesture &&
                            dragValue.interactionKind == InteractionKind.DirectPinch
                    ) {
                        Log.i(
                            INTERACTION_TAG,
                            "boardMoveEnd position=${scene.boardPositionForLog()}",
                        )
                    }
                    Log.i(
                        INTERACTION_TAG,
                        "spatialDragEnd mode=$mode kind=${dragValue.interactionKind}",
                    )
                    if (feelHoldRotateActive) {
                        Log.i(INTERACTION_TAG, "feelHoldRotateEnd")
                    }
                    feelCardDragGesture = false
                    feelHoldRotateActive = false
                },
                onDragCancel = { dragValue ->
                    Log.i(
                        INTERACTION_TAG,
                        "spatialDragCancel mode=$mode kind=${dragValue.interactionKind}",
                    )
                    feelHoldRotateActive = false
                },
            )
        }

        tapModifier = tapModifier.pointerInput(root, mode, cellByEntity) {
            detectSpatialTapGesture(
                context = context,
                targetedToEntity = TargetEntity.hit(root),
            ) { tap ->
                val accepted = if (mode == GameMode.FEEL) {
                    isFeelSelectionTap(tap.interactionKind)
                } else {
                    tap.interactionKind == InteractionKind.GazePinch
                }
                Log.i(
                    INTERACTION_TAG,
                    "spatialTap mode=$mode kind=${tap.interactionKind} " +
                        "entity=${tap.targetEntity?.getName()} accepted=$accepted",
                )
                if (accepted) {
                    val cell = cellByEntity[tap.targetEntity]
                    if (cell != null) {
                        if (mode == GameMode.FEEL) {
                            onFeelCardTap(
                                cell = cell,
                                tapPosition = tap.position,
                            )
                        } else {
                            onCardTap(cell)
                        }
                    } else {
                        Log.w(INTERACTION_TAG, "Spatial tap did not map to a card cell")
                    }
                }
            }
        }
    }

    // Detached 3D controls can be rearranged by the player. This recognizer is
    // separate from the board recognizer, so moving a control never orbits the
    // volumetric board.
    if (!menuOpen) tapModifier = tapModifier.pointerInput(mode, cellByEntity) {
        detectSpatialTapGesture(
            context = context,
            targetedToEntity = TargetEntity.any { entity ->
                scene.isFloatingControl(entity)
            },
        ) { tap ->
            val controlId = scene.floatingControlId(tap.targetEntity)
            Log.i(
                INTERACTION_TAG,
                "controlSpatialTap kind=${tap.interactionKind} " +
                    "id=$controlId entity=${tap.targetEntity?.getName()}",
            )
            if (tap.interactionKind != InteractionKind.Unknown) {
                onFloatingControlTap(controlId)
            }
        }
    }

    var draggedControlId: String? = null
    var controlDragMoved = false
    var controlDragDistance = 0f
    var controlDragStartedAt = 0L
    var pendingControlX = 0f
    var pendingControlY = 0f
    var pendingControlZ = 0f

    if (!menuOpen) tapModifier = tapModifier.pointerInput(mode, cellByEntity) {
        detectSpatialDragGesture(
            context = context,
            targetedToEntity = TargetEntity.any { entity ->
                scene.isFloatingControl(entity)
            },
            onDragStart = { _, dragValue ->
                draggedControlId = scene.beginFloatingControlDrag(dragValue.targetEntity)
                controlDragMoved = false
                controlDragDistance = 0f
                controlDragStartedAt = System.currentTimeMillis()
                pendingControlX = 0f
                pendingControlY = 0f
                pendingControlZ = 0f
                Log.i(
                    INTERACTION_TAG,
                    "controlDragStart id=$draggedControlId " +
                        "entity=${dragValue.targetEntity?.getName()}",
                )
            },
            onDrag = { dragValue ->
                if (!controlDragMoved) {
                    pendingControlX += dragValue.dragAmount.x
                    pendingControlY += dragValue.dragAmount.y
                    pendingControlZ += dragValue.dragAmount.z
                    controlDragDistance = kotlin.math.sqrt(
                        pendingControlX * pendingControlX +
                            pendingControlY * pendingControlY +
                            pendingControlZ * pendingControlZ,
                    )

                    // Treat small movement as a tap. This prevents a pinch
                    // from nudging the button before its action fires.
                    if (
                        controlDragDistance >= 14f &&
                            draggedControlId != null
                    ) {
                        controlDragMoved = true
                        scene.moveFloatingControl(
                            id = draggedControlId!!,
                            delta = Vector3(
                                x = pendingControlX * metersPerPixel,
                                y = -pendingControlY * metersPerPixel,
                                z = pendingControlZ * metersPerPixel,
                            ),
                        )
                        pendingControlX = 0f
                        pendingControlY = 0f
                        pendingControlZ = 0f
                    }
                } else {
                    draggedControlId?.let { id ->
                        scene.moveFloatingControl(
                            id = id,
                            delta = Vector3(
                                x = dragValue.dragAmount.x * metersPerPixel,
                                y = -dragValue.dragAmount.y * metersPerPixel,
                                z = dragValue.dragAmount.z * metersPerPixel,
                            ),
                        )
                    }
                }
            },
            onDragEnd = {
                draggedControlId?.let { id ->
                    scene.endFloatingControlDrag(id)
                    val elapsed = System.currentTimeMillis() - controlDragStartedAt
                    if (!controlDragMoved && elapsed < 450f) {
                        Log.i(
                            INTERACTION_TAG,
                            "controlDragTooSmall id=$id elapsed=$elapsed",
                        )
                    } else {
                        persistControlPositions()
                        Log.i(INTERACTION_TAG, "controlDragEnd id=$id saved=true")
                    }
                }
                draggedControlId = null
            },
            onDragCancel = {
                draggedControlId?.let { id ->
                    scene.endFloatingControlDrag(id)
                    persistControlPositions()
                    Log.w(INTERACTION_TAG, "controlDragCancel id=$id saved=true")
                }
                draggedControlId = null
            },
        )
    }

    SpatialView(
        modifier = tapModifier,
        update = { content, attachments ->
            if (menuPanelEntity == null) {
                attachments.entity(id = "game_menu")?.apply {
                    enabled = menuOpen
                    content.addEntity(this)
                    menuPanelEntity = this
                    setupPanelPlacement.bind(this)
                }
            }
            if (winPanelEntity == null) {
                attachments.entity(id = "game_win")?.apply {
                    enabled = false
                    components[TransformComponent::class.java]?.setPosition(
                        Vector3(
                            x = 0f,
                            y = STAGE_CONTENT_Y - 0.05f,
                            z = STAGE_CONTENT_Z + 0.30f,
                        ),
                    )
                    content.addEntity(this)
                    winPanelEntity = this
                }
            }
            if (failPanelEntity == null) {
                attachments.entity(id = "game_fail")?.apply {
                    enabled = false
                    components[TransformComponent::class.java]?.setPosition(
                        Vector3(
                            x = 0f,
                            y = 0.72f,
                            z = -0.70f,
                        ),
                    )
                    content.addEntity(this)
                    failPanelEntity = this
                }
            }
            if (toolsPanelEntity == null) {
                attachments.entity(id = "game_tools")?.apply {
                    components[TransformComponent::class.java]?.apply {
                        setPosition(
                            clampedToolRailPosition(toolRailPosition),
                        )
                        // Keep the AttachmentPanel's authored front face. The
                        // hardware-specific negative TOOL_RAIL_Z now places this
                        // face toward the player; the previous +Z position showed
                        // its mirrored back face.
                        setEulerAngles(toolPanelRotation())
                    }
                    content.addEntity(this)
                    toolsPanelEntity = this
                }
            }
        },
        initial = { content, attachments ->
            if (boardRoot == null) {
                scene.build(game)
                cards = scene.cards.toMap()
                boardRoot = scene.boardRoot
            }

            // SpatialView may re-run initial with a new content root when its
            // composition is recreated. Build once, but reattach persistent
            // ECS roots every time; otherwise only AttachmentPanels remain
            // visible and the board/cosmos disappear from the new content.
            val root = requireNotNull(scene.boardRoot)
            content.addEntity(cosmosScene.root)
            content.addEntity(root)
            scene.celebrationRoot?.let { celebration ->
                celebration.components[TransformComponent::class.java]?.setPosition(
                    Vector3(
                        x = 0f,
                        y = STAGE_CONTENT_Y,
                        z = STAGE_CONTENT_Z,
                    ),
                )
                content.addEntity(celebration)
            }

            attachments.entity(id = "game_menu")?.apply {
                // SpatialView.initial can re-run while the player is changing
                // difficulty or mode. Preserve the current expanded state;
                // otherwise the sheet closes itself immediately after opening.
                enabled = menuOpen
                content.addEntity(this)
                menuPanelEntity = this
                setupPanelPlacement.bind(this)
            }
            attachments.entity(id = "game_win")?.apply {
                enabled = false
                components[TransformComponent::class.java]?.setPosition(
                    Vector3(
                        x = 0f,
                        y = STAGE_CONTENT_Y - 0.05f,
                        z = STAGE_CONTENT_Z + 0.30f,
                    ),
                )
                content.addEntity(this)
                winPanelEntity = this
            }
            attachments.entity(id = "game_fail")?.apply {
                enabled = false
                components[TransformComponent::class.java]?.setPosition(
                    Vector3(
                        x = 0f,
                        y = 0.72f,
                        z = -0.70f,
                    ),
                )
                content.addEntity(this)
                failPanelEntity = this
            }
            attachments.entity(id = "game_tools")?.apply {
                components[TransformComponent::class.java]?.apply {
                    setPosition(
                        clampedToolRailPosition(toolRailPosition),
                    )
                    setEulerAngles(toolPanelRotation())
                }
                content.addEntity(this)
                toolsPanelEntity = this
            }
        },
        attachments = {
            AttachmentPanel(
                id = "game_menu",
                size = setupPanelSize,
            ) {
                GameSetupPanel(
                    panelEntity = menuPanelEntity,
                    appLanguage = appLanguage,
                    onToggleLanguage = onToggleLanguage,
                    selectedDifficulty = selectedDifficulty,
                    onDifficultyChange = { selectedDifficulty = it },
                    customRows = customRows,
                    customCols = customCols,
                    customLayers = customLayers,
                    onCustomBoardChange = { rows, cols, layers ->
                        customRows = rows
                        customCols = cols
                        customLayers = layers
                    },
                    selectedMode = selectedSetupMode,
                    onModeChange = { selectedSetupMode = it },
                    isInitialSetup = false,
                    onDrag = setupPanelPlacement::move,
                    onRotate = setupPanelPlacement::rotate,
                    onStart = { spec ->
                        mode = selectedSetupMode
                        activeBoardSpec = spec
                        activeGameKey++
                        menuOpen = false
                    },
                    onClose = { menuOpen = false },
                    onExit = ::exitGame,
                )
            }

            AttachmentPanel(
                id = "game_win",
                size = winPanelSize,
            ) {
                WinPanel(
                    nextSpec = currentBoardSpec.nextLevel,
                    actionsReady = winActionsReady,
                    onNextLevel = ::startNextLevel,
                    onReturnMenu = ::returnToMenu,
                )
            }

            AttachmentPanel(
                id = "game_fail",
                size = failPanelSize,
            ) {
                if (timedOut) {
                    TimedOutPanel(
                        remainingPairs = remainingPairs,
                        onRetry = ::retryTimedChallenge,
                        onReturnMenu = ::returnToMenu,
                    )
                } else {
                    TimedChallengePanel(
                        remainingTimeMillis = remainingTimeMillis,
                        remainingPairs = remainingPairs,
                        totalTimeMillis = timedLimitMillis,
                    )
                }
            }

            AttachmentPanel(
                id = "game_tools",
                size = toolsPanelSize,
            ) {
                CosmosControlRail(
                    hintsRemaining = hintsRemaining,
                    isShuffling = isShuffling,
                    cleared = cleared,
                    onHoverSound = { matchSound.uiHover() },
                    onClickSound = { matchSound.uiClick() },
                    onDrag = ::moveToolRail,
                    onRotate = ::rotateToolPanel,
                    onOpenMenu = {
                        clearSelection()
                        clearHintHighlight()
                        selectedDifficulty = when (currentBoardSpec) {
                            BoardSpec.EASY -> DifficultyChoice.PRESET_3
                            BoardSpec.NORMAL -> DifficultyChoice.PRESET_4
                            BoardSpec.HARD -> DifficultyChoice.PRESET_5
                            else -> DifficultyChoice.CUSTOM
                        }
                        customRows = currentBoardSpec.rows
                        customCols = currentBoardSpec.cols
                        customLayers = currentBoardSpec.layers
                        selectedSetupMode = mode
                        bringSetupPanelToFront()
                        menuOpen = true
                    },
                    onHint = ::requestHint,
                    onShuffle = ::shuffleBoard,
                    onGiveUp = ::restart,
                )
            }
        },
    )
}
}
@Composable
private fun InitialSetupVolume(
    appLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    selectedDifficulty: DifficultyChoice,
    onDifficultyChange: (DifficultyChoice) -> Unit,
    customRows: Int,
    customCols: Int,
    customLayers: Int,
    onCustomBoardChange: (Int, Int, Int) -> Unit,
    selectedMode: GameMode,
    onModeChange: (GameMode) -> Unit,
    isInitialSetup: Boolean,
    onStart: (BoardSpec) -> Unit,
    onClose: () -> Unit,
    placement: SetupPanelPlacement,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val containerMeters = LocalPhysicalLengthConverter.current.dpToLength(
        960.dp,
        LengthUnit.Meters,
    )
    val setupPanelSize = panelSize(
        SETUP_PANEL_WIDTH,
        SETUP_PANEL_HEIGHT,
        LengthUnit.Meters,
    )
    val directMenuLabels = DirectMenuLabels(
        title = stringResource(R.string.setup_title),
        subtitle = "${stringResource(R.string.difficulty_normal_title)} · " +
            stringResource(R.string.mode_casual_title),
        rule = stringResource(R.string.setup_rule_short),
        casualLine = stringResource(R.string.mode_casual_subtitle),
        menuHint = stringResource(R.string.direct_menu_switch_hint),
        start = stringResource(R.string.setup_start),
        exit = stringResource(R.string.setup_exit),
        language = stringResource(R.string.language_toggle),
    )
    val cosmosScene = remember(containerMeters) {
        CosmosBackgroundSceneState()
    }
    val directMenuScene = remember(appLanguage) { DirectSetupMenuSceneState() }
    val setupPanelScene = remember { SetupPanelSceneState() }
    var boundPanelEntity by remember { mutableStateOf<Entity?>(null) }
    val isEmulator = Build.HARDWARE.equals("ranchu", ignoreCase = true)

    DisposableEffect(cosmosScene) {
        onDispose {
            cosmosScene.destroy()
        }
    }
    LaunchedEffect(cosmosScene) {
        while (true) {
            cosmosScene.update(0.033f)
            delay(33L)
        }
    }
    DisposableEffect(directMenuScene) {
        onDispose {
            directMenuScene.destroy()
        }
    }
    DisposableEffect(setupPanelScene) {
        onDispose {
            setupPanelScene.destroy()
        }
    }

    DisposableEffect(boundPanelEntity) {
        onDispose {
            placement.clearIfCurrent(boundPanelEntity)
        }
    }

    var menuModifier: Modifier = Modifier
    menuModifier = if (isEmulator) {
        menuModifier.pointerInput(directMenuScene) {
            detectSpatialTapGesture(
                context = context,
                targetedToEntity = TargetEntity.any { entity ->
                    directMenuScene.action(entity) != null
                },
            ) { tap ->
                val action = directMenuScene.action(tap.targetEntity)
                Log.i(
                    INTERACTION_TAG,
                    "directMenuTap kind=${tap.interactionKind} action=$action",
                )
                when (action) {
                    "start" -> onStart(BoardSpec.NORMAL)
                    "exit" -> onExit()
                    "language" -> onToggleLanguage()
                }
            }
        }
    } else {
        menuModifier
    }

    key(appLanguage) {
        SpatialView(
        modifier = menuModifier,
        initial = { content, attachments ->
            content.addEntity(cosmosScene.root)
            if (isEmulator) {
                directMenuScene.build(directMenuLabels)
                content.addEntity(requireNotNull(directMenuScene.root))
            } else {
                attachments.entity(id = "initial_setup")?.apply {
                    content.addEntity(this)
                    placement.bind(this)
                    boundPanelEntity = this
                } ?: Log.w(INTERACTION_TAG, "initial setup attachment missing")
            }
        },
        attachments = {
            if (!isEmulator) {
                AttachmentPanel(
                    id = "initial_setup",
                    size = setupPanelSize,
                ) {
                    GameSetupPanel(
                        panelEntity = boundPanelEntity,
                        appLanguage = appLanguage,
                        onToggleLanguage = onToggleLanguage,
                        selectedDifficulty = selectedDifficulty,
                        onDifficultyChange = onDifficultyChange,
                        customRows = customRows,
                        customCols = customCols,
                        customLayers = customLayers,
                        onCustomBoardChange = onCustomBoardChange,
                        selectedMode = selectedMode,
                        onModeChange = onModeChange,
                        isInitialSetup = isInitialSetup,
                        onDrag = placement::move,
                        onRotate = placement::rotate,
                        onStart = onStart,
                        onClose = onClose,
                        onExit = onExit,
                    )
                }
            }
        },
        )
    }
}

@Composable
private fun GameSetupPanel(
    panelEntity: Entity?,
    appLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    selectedDifficulty: DifficultyChoice,
    onDifficultyChange: (DifficultyChoice) -> Unit,
    customRows: Int,
    customCols: Int,
    customLayers: Int,
    onCustomBoardChange: (Int, Int, Int) -> Unit,
    selectedMode: GameMode,
    onModeChange: (GameMode) -> Unit,
    isInitialSetup: Boolean,
    onDrag: (Vector3) -> Unit,
    onRotate: (yawDeltaDegrees: Float, pitchDeltaDegrees: Float) -> Unit,
    onStart: (BoardSpec) -> Unit,
    onClose: () -> Unit,
    onExit: () -> Unit,
) {
    val panelShape = RoundedCornerShape(34.dp)
    val directDragModifier = Modifier.setupPanelDrag(
        panelEntity = panelEntity,
        onPanelPositionChange = onDrag,
    )
    LaunchedEffect(panelEntity) {
        Log.i(
            INTERACTION_TAG,
            "setupPanelGestureBound entity=${panelEntity?.getName()} valid=${panelEntity?.valid}",
        )
    }
    val selectedSpec = when (selectedDifficulty) {
        DifficultyChoice.PRESET_3 -> BoardSpec.EASY
        DifficultyChoice.PRESET_4 -> BoardSpec.NORMAL
        DifficultyChoice.PRESET_5 -> BoardSpec.HARD
        DifficultyChoice.CUSTOM -> BoardSpec(customRows, customCols, customLayers)
    }
    val selectedPairText = stringResource(
        R.string.game_pair_count,
        selectedSpec.pairCount,
    )
    val selectedBlankText = stringResource(R.string.game_blank_cell)
    val selectedSummary = buildString {
        append("${selectedSpec.label} · $selectedPairText")
        if (selectedSpec.hasBlankCell) {
            append(" · $selectedBlankText")
        }
        if (selectedMode == GameMode.TIMED) {
            append(
                " · ${formatDuration(
                    LianliankanGame.timedChallengeDurationMillis(selectedSpec.pairCount),
                )}",
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .then(directDragModifier)
            .clip(panelShape)
            .background(
                color = Color(0xFF050A16), // design-style: fixed-figma-color linklin opaque cosmos menu
                shape = panelShape,
            )
            .border(
                width = 1.5.dp,
                color = Color(0x5C8ACBFF), // design-style: fixed-figma-color linklin starlight border
                shape = panelShape,
            )
            .padding(horizontal = 15.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            color = PicoTheme.colorScheme.labelPrimaryLight,
            style = PicoTheme.typography.titleLarge.copy(
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(3.dp))
        LanguageSwitchButton(
            text = stringResource(R.string.language_toggle),
            onClick = onToggleLanguage,
        )
        Spacer(Modifier.size(4.dp))
        SetupSectionLabel(stringResource(R.string.setup_difficulty))
        Spacer(Modifier.size(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            SetupChoiceCard(
                title = stringResource(R.string.difficulty_easy_title),
                subtitle = stringResource(R.string.difficulty_easy_subtitle),
                selected = selectedDifficulty == DifficultyChoice.PRESET_3,
                accent = PicoTheme.colorScheme.fillSecondary,
                modifier = Modifier.weight(1f),
            ) { onDifficultyChange(DifficultyChoice.PRESET_3) }
            SetupChoiceCard(
                title = stringResource(R.string.difficulty_normal_title),
                subtitle = stringResource(R.string.difficulty_normal_subtitle),
                selected = selectedDifficulty == DifficultyChoice.PRESET_4,
                accent = PicoTheme.colorScheme.fillSecondary,
                modifier = Modifier.weight(1f),
            ) { onDifficultyChange(DifficultyChoice.PRESET_4) }
        }
        Spacer(Modifier.size(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            SetupChoiceCard(
                title = stringResource(R.string.difficulty_hard_title),
                subtitle = stringResource(R.string.difficulty_hard_subtitle),
                selected = selectedDifficulty == DifficultyChoice.PRESET_5,
                accent = PicoTheme.colorScheme.fillSecondary,
                modifier = Modifier.weight(1f),
            ) { onDifficultyChange(DifficultyChoice.PRESET_5) }
            SetupChoiceCard(
                title = stringResource(R.string.difficulty_custom_title),
                subtitle = stringResource(R.string.difficulty_custom_subtitle),
                selected = selectedDifficulty == DifficultyChoice.CUSTOM,
                accent = PicoTheme.colorScheme.fillSecondary,
                modifier = Modifier.weight(1f),
            ) { onDifficultyChange(DifficultyChoice.CUSTOM) }
        }

        if (selectedDifficulty == DifficultyChoice.CUSTOM) {
            Spacer(Modifier.size(5.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DimensionStepper(
                    label = stringResource(R.string.custom_rows),
                    value = customRows,
                    modifier = Modifier.weight(1f),
                ) { value ->
                    onCustomBoardChange(value, customCols, customLayers)
                }
                DimensionStepper(
                    label = stringResource(R.string.custom_cols),
                    value = customCols,
                    modifier = Modifier.weight(1f),
                ) { value ->
                    onCustomBoardChange(customRows, value, customLayers)
                }
                DimensionStepper(
                    label = stringResource(R.string.custom_layers),
                    value = customLayers,
                    modifier = Modifier.weight(1f),
                ) { value ->
                    onCustomBoardChange(customRows, customCols, value)
                }
            }
        }

        Spacer(Modifier.size(6.dp))
        SetupSectionLabel(stringResource(R.string.setup_mode))
        Spacer(Modifier.size(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            SetupChoiceCard(
                title = stringResource(R.string.mode_casual_title),
                subtitle = stringResource(R.string.mode_casual_subtitle),
                selected = selectedMode == GameMode.CASUAL,
                accent = PicoTheme.colorScheme.fillTertiary,
                leadingEmoji = "😌",
                modifier = Modifier.weight(1f),
            ) {
                onModeChange(GameMode.CASUAL)
            }
            SetupChoiceCard(
                title = stringResource(R.string.mode_feel_title),
                subtitle = stringResource(R.string.mode_feel_subtitle),
                selected = selectedMode == GameMode.FEEL,
                accent = PicoTheme.colorScheme.fillTertiary,
                leadingEmoji = "🤝",
                modifier = Modifier.weight(1f),
            ) {
                onModeChange(GameMode.FEEL)
            }
            SetupChoiceCard(
                title = stringResource(R.string.mode_timed_title),
                subtitle = stringResource(R.string.mode_timed_subtitle),
                selected = selectedMode == GameMode.TIMED,
                accent = PicoTheme.colorScheme.fillTertiary,
                leadingEmoji = "⏳",
                modifier = Modifier.weight(1f),
            ) {
                onModeChange(GameMode.TIMED)
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(
                when (selectedMode) {
                    GameMode.FEEL -> R.string.feel_mode_instruction
                    GameMode.TIMED -> R.string.timed_mode_instruction
                    GameMode.CASUAL -> R.string.setup_rule_short
                },
            ),
            color = Color(0xFFA9B8CE), // design-style: fixed-figma-color linklin cosmic secondary text
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                lineHeight = 19.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = selectedSummary,
            color = PicoTheme.colorScheme.alert,
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SetupPrimaryButton(
                text = stringResource(
                    if (isInitialSetup) R.string.setup_start else R.string.setup_continue,
                ),
                onClick = { onStart(selectedSpec) },
                modifier = Modifier.weight(2.25f),
            )
            SetupExitButton(
                text = stringResource(R.string.setup_exit),
                onClick = onExit,
                modifier = Modifier.weight(1f),
            )
        }
        if (!isInitialSetup) {
            Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.setup_close),
            color = Color(0xFFA9B8CE), // design-style: fixed-figma-color linklin cosmic secondary text
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LanguageSwitchButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = PicoTheme.colorScheme.fillSecondary,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(34.dp)
            .width(164.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SetupSectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFFA9C3E8), // design-style: fixed-figma-color linklin starlight label
        style = PicoTheme.typography.bodyLarge.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color = Color(0x248ACBFF)) // design-style: fixed-figma-color linklin starlight chip
            .padding(horizontal = 12.dp, vertical = 3.dp),
    )
}

@Composable
private fun SetupChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    leadingEmoji: String? = null,
    locked: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val indication = LocalIndication.current
    val cardShape = RoundedCornerShape(24.dp)
    val backgroundColor = when {
        locked -> Color(0xFF0C1424) // design-style: fixed-figma-color linklin cosmic locked surface
        selected -> accent.copy(alpha = 0.30f)
        else -> Color(0xFF101A2B) // design-style: fixed-figma-color linklin cosmic surface
    }
    val contentColor = if (locked) {
        Color(0xFF76829A) // design-style: fixed-figma-color linklin cosmic locked text
    } else {
        Color(0xFFF4F8FF) // design-style: fixed-figma-color linklin cosmic text
    }
    val borderColor = when {
        selected -> accent.copy(alpha = 0.82f)
        locked -> Color(0xFF263247) // design-style: fixed-figma-color linklin cosmic locked border
        else -> Color(0xFF283A55) // design-style: fixed-figma-color linklin cosmic border
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(if (leadingEmoji == null) 50.dp else 56.dp)
            .graphicsLayer {
                scaleX = if (selected) 1.02f else 1f
                scaleY = if (selected) 1.02f else 1f
                translationY = if (pressed) 3f else 0f
                alpha = if (locked) 0.50f else 1f
            }
            .clip(cardShape)
            .then(
                if (!locked) {
                    Modifier.spatialHoverEffect(
                        block = { context ->
                            if (context.isActive) {
                                scale(1.05f)
                                offset(y = 2.dp)
                            }
                        },
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = !locked,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = indication,
            )
            .background(color = backgroundColor, shape = cardShape)
            .border(
                width = if (selected) 3.dp else 1.5.dp,
                color = borderColor,
                shape = cardShape,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (leadingEmoji != null) {
                Text(
                    text = leadingEmoji,
                    color = Color.Unspecified,
                    fontSize = 24.sp,
                    lineHeight = 27.sp,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = contentColor,
                    style = PicoTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    color = if (locked) contentColor else Color(0xFFA6B4C8), // design-style: fixed-figma-color linklin cosmic secondary text
                    style = PicoTheme.typography.bodyLarge.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SetupPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val indication = LocalIndication.current
    val buttonShape = RoundedCornerShape(28.dp)
    val backgroundColor = if (pressed) {
        PicoTheme.colorScheme.lightenPressed
    } else {
        PicoTheme.colorScheme.fillPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .graphicsLayer {
                translationY = if (pressed) 4f else 0f
            }
            .clip(buttonShape)
            .spatialHoverEffect(
                block = { context ->
                    if (context.isActive) {
                        scale(1.05f)
                        alpha(1f)
                    } else {
                        alpha(0.96f)
                    }
                }
            )
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = indication,
            )
            .background(color = backgroundColor, shape = buttonShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 21.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SetupExitButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val indication = LocalIndication.current
    val buttonShape = RoundedCornerShape(24.dp)
    val backgroundColor = if (pressed) {
        Color(0xFF684246) // design-style: fixed-figma-color linklin pressed cosmic coral
    } else {
        Color(0xFF2A1620) // design-style: fixed-figma-color linklin cosmic coral surface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .graphicsLayer {
                translationY = if (pressed) 4f else 0f
            }
            .clip(buttonShape)
            .spatialHoverEffect(
                block = { context ->
                    if (context.isActive) {
                        scale(1.04f)
                    }
                },
            )
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = indication,
            )
            .background(color = backgroundColor, shape = buttonShape)
            .border(
                width = 1.dp,
                color = Color(0x66FF9E9E), // design-style: fixed-figma-color linklin coral border
                shape = buttonShape,
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = Color(0xFFFFC9C9), // design-style: fixed-figma-color linklin cosmic coral text
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DimensionStepper(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = Color(0xFFA9B8CE), // design-style: fixed-figma-color linklin cosmic secondary text
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SetupStepButton("−") { onValueChange((value - 1).coerceAtLeast(2)) }
            Spacer(Modifier.size(6.dp))
            Text(
                text = value.toString(),
                color = Color(0xFFF4F8FF), // design-style: fixed-figma-color linklin cosmic text
                style = PicoTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(6.dp))
            SetupStepButton("+") { onValueChange(value + 1) }
        }
    }
}

@Composable
private fun SetupStepButton(
    symbol: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(14.dp))
            .spatialHoverEffect(
                block = { context ->
                    if (context.isActive) {
                        scale(1.08f)
                    }
                },
            )
            .clickable(onClick = onClick)
            .background(Color(0xFF172638)) // design-style: fixed-figma-color linklin cosmic surface
            .border(
                width = 1.dp,
                color = Color(0x4D8ACBFF), // design-style: fixed-figma-color linklin starlight border
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        Text(
            text = symbol,
            color = Color(0xFFA9C3E8), // design-style: fixed-figma-color linklin starlight label
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

private fun Color.darken(fraction: Float): Color =
    Color(
        red = red * (1f - fraction),
        green = green * (1f - fraction),
        blue = blue * (1f - fraction),
        alpha = alpha,
    )

@Composable
private fun TimedChallengePanel(
    remainingTimeMillis: Long,
    remainingPairs: Int,
    totalTimeMillis: Long,
) {
    val timeRatio = if (totalTimeMillis > 0L) {
        remainingTimeMillis.toFloat() / totalTimeMillis.toFloat()
    } else {
        0f
    }
    val warning = timeRatio <= 0.50f
    val urgent = timeRatio <= 0.20f
    val urgentPhase = (remainingTimeMillis % 1000L) / 1000f
    val urgentPulse = if (urgent) {
        1f + 0.028f * sin(2f * PI.toFloat() * urgentPhase)
    } else {
        1f
    }
    val panelShape = RoundedCornerShape(20.dp)
    val stateColor = when {
        urgent -> Color(0xFFFF9E9E) // design-style: fixed-figma-color linklin urgent coral
        warning -> Color(0xFFFFE08A) // design-style: fixed-figma-color linklin warning gold
        else -> Color(0xFF8ACBFF) // design-style: fixed-figma-color linklin calm blue
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = urgentPulse
                scaleY = urgentPulse
            }
            .clip(panelShape)
            .background(
                color = Color(0xFF050A16), // design-style: fixed-figma-color linklin opaque cosmos menu
                shape = panelShape,
            )
            .border(
                width = if (urgent) 2.dp else 1.dp,
                color = stateColor.copy(alpha = if (urgent) 0.68f else 0.48f),
                shape = panelShape,
            )
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(
            text = "⏳",
            color = Color.Unspecified,
            fontSize = 17.sp,
            lineHeight = 20.sp,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.game_timer_label),
                color = Color(0xFFA9B8CE), // design-style: fixed-figma-color linklin cosmic secondary text
                style = PicoTheme.typography.bodyLarge.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                text = formatDuration(remainingTimeMillis),
                color = stateColor,
                style = PicoTheme.typography.bodyLarge.copy(
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.game_remaining_short),
                color = Color(0xFFA9B8CE), // design-style: fixed-figma-color linklin cosmic secondary text
                style = PicoTheme.typography.bodyLarge.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    R.string.game_pair_count,
                    remainingPairs,
                ),
                color = Color(0xFFF4F8FF), // design-style: fixed-figma-color linklin cosmic text
                style = PicoTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TimedOutPanel(
    remainingPairs: Int,
    onRetry: () -> Unit,
    onReturnMenu: () -> Unit,
) {
    val panelShape = RoundedCornerShape(24.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(panelShape)
            .background(
                color = Color(0xFF050A16), // design-style: fixed-figma-color linklin opaque cosmos menu
                shape = panelShape,
            )
            .border(
                width = 1.5.dp,
                color = Color(0x80FF9E9E), // design-style: fixed-figma-color linklin urgent coral
                shape = panelShape,
            )
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            text = "⏱",
            color = Color.Unspecified,
            fontSize = 15.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.size(3.dp))
        Text(
            text = stringResource(R.string.game_time_up),
            color = PicoTheme.colorScheme.labelPrimaryLight,
            style = PicoTheme.typography.titleLarge.copy(
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.game_time_up_subtitle, remainingPairs),
            color = Color(0xFFA9B8CE), // design-style: fixed-figma-color linklin cosmic secondary text
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 13.sp,
                lineHeight = 17.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(9.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WinActionButton(
                text = stringResource(R.string.game_retry),
                containerColor = PicoTheme.colorScheme.fillPrimary,
                contentColor = Color.White,
                modifier = Modifier.weight(1.10f),
                onClick = onRetry,
            )
            WinActionButton(
                text = stringResource(R.string.game_return_menu),
                containerColor = Color(0xFF101A2B), // design-style: fixed-figma-color linklin cosmic surface
                contentColor = Color(0xFFA9C3E8), // design-style: fixed-figma-color linklin starlight label
                modifier = Modifier.weight(1f),
                onClick = onReturnMenu,
            )
        }
    }
}

@Composable
private fun WinPanel(
    nextSpec: BoardSpec,
    actionsReady: Boolean,
    onNextLevel: () -> Unit,
    onReturnMenu: () -> Unit,
) {
    val panelShape = RoundedCornerShape(24.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(panelShape)
            .background(
                color = Color(0xFF050A16), // design-style: fixed-figma-color linklin opaque cosmos menu
                shape = panelShape,
            )
            .border(
                width = 1.5.dp,
                color = Color(0x5C8ACBFF), // design-style: fixed-figma-color linklin starlight border
                shape = panelShape,
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
    ) {
        Text(
            text = "✦ ✦ ✦",
            color = Color(0xFFFFE08A), // design-style: fixed-figma-color linklin starlight gold
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(3.dp))
        Text(
            text = stringResource(R.string.game_win),
            color = PicoTheme.colorScheme.labelPrimaryLight,
            style = PicoTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.game_win_subtitle),
            color = Color(0xFFA9B8CE), // design-style: fixed-figma-color linklin cosmic secondary text
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 13.sp,
                lineHeight = 17.sp,
            ),
            textAlign = TextAlign.Center,
        )
        if (!actionsReady) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.game_win_waiting_actions),
                color = Color(0xFF8FA3BD), // design-style: fixed-figma-color linklin cosmic tertiary text
                style = PicoTheme.typography.bodyLarge.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                textAlign = TextAlign.Center,
            )
        } else {
            Spacer(Modifier.size(9.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                WinActionButton(
                    text = stringResource(R.string.game_next_level, nextSpec.label),
                    containerColor = PicoTheme.colorScheme.fillPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1.18f),
                    onClick = onNextLevel,
                )
                WinActionButton(
                    text = stringResource(R.string.game_return_menu),
                    containerColor = Color(0xFF101A2B), // design-style: fixed-figma-color linklin cosmic surface
                    contentColor = Color(0xFFA9C3E8), // design-style: fixed-figma-color linklin starlight label
                    modifier = Modifier.weight(1f),
                    onClick = onReturnMenu,
                )
            }
        }
    }
}

@Composable
private fun WinActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        size = ButtonDefaults.buttonSize(
            width = 102.dp,
            height = 42.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(17.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = contentColor,
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CosmosControlRail(
    hintsRemaining: Int,
    isShuffling: Boolean,
    cleared: Boolean,
    onHoverSound: () -> Unit,
    onClickSound: () -> Unit,
    onDrag: (Vector3) -> Unit,
    onRotate: (yawDeltaDegrees: Float, pitchDeltaDegrees: Float) -> Unit,
    onOpenMenu: () -> Unit,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onGiveUp: () -> Unit,
) {
    val context = LocalContext.current
    val directDragModifier = Modifier.directPanelGesture(
        logName = "toolRail",
        onMove = onDrag,
        onRotate = onRotate,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 7.dp,
            alignment = Alignment.CenterVertically,
        ),
        modifier = Modifier
            .fillMaxSize()
            .then(directDragModifier),
    ) {
        CosmosRailButton(
            iconRes = R.drawable.ic_control_menu,
            text = stringResource(R.string.menu_summary),
            accentColor = Color(0xFFFFB347), // design-style: fixed-figma-color linklin warm star
            onHoverSound = onHoverSound,
            onClickSound = onClickSound,
            onClick = onOpenMenu,
        )

        CosmosRailButton(
            iconRes = R.drawable.ic_control_hint,
            text = stringResource(R.string.game_hint_count, hintsRemaining),
            accentColor = Color(0xFFFFE08A), // design-style: fixed-figma-color linklin starlight gold
            enabled = hintsRemaining > 0 && !isShuffling && !cleared,
            onHoverSound = onHoverSound,
            onClickSound = onClickSound,
            onClick = onHint,
        )
        CosmosRailButton(
            iconRes = R.drawable.ic_control_shuffle,
            text = stringResource(
                if (isShuffling) {
                    R.string.shuffle_in_progress_short
                } else {
                    R.string.game_shuffle
                },
            ),
            accentColor = Color(0xFF8ACBFF), // design-style: fixed-figma-color linklin nebula blue
            enabled = !isShuffling && !cleared,
            onHoverSound = onHoverSound,
            onClickSound = onClickSound,
            onClick = onShuffle,
        )
        CosmosRailButton(
            iconRes = R.drawable.ic_control_give_up,
            text = stringResource(R.string.game_give_up),
            accentColor = Color(0xFFFF9E9E), // design-style: fixed-figma-color linklin soft coral
            enabled = !isShuffling && !cleared,
            onHoverSound = onHoverSound,
            onClickSound = onClickSound,
            onClick = onGiveUp,
        )
    }
}

@Composable
private fun CosmosRailButton(
    iconRes: Int,
    text: String,
    accentColor: Color,
    onClick: () -> Unit,
    onHoverSound: () -> Unit,
    onClickSound: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) {
        Color(0xFFF4F8FF) // design-style: fixed-figma-color linklin starlight text
    } else {
        Color(0xFF8A93A5) // design-style: fixed-figma-color linklin dimmed control text
    }
    var pointerHovered by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (enabled) {
                onClickSound()
                onClick()
            }
        },
        enabled = enabled,
        modifier = modifier.pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val inside = event.changes.any { change ->
                        change.position.x >= 0f &&
                            change.position.y >= 0f &&
                            change.position.x <= size.width.toFloat() &&
                            change.position.y <= size.height.toFloat()
                    }
                    if (inside && !pointerHovered && enabled) {
                        onHoverSound()
                    }
                    if (inside || pointerHovered) {
                        pointerHovered = inside
                    }
                }
            }
        },
        size = ButtonDefaults.buttonSize(
            width = 112.dp,
            height = 48.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) {
                Color(0x30FFFFFF) // design-style: fixed-figma-color linklin cosmic glass
            } else {
                Color(0x14FFFFFF) // design-style: fixed-figma-color linklin cosmic glass
            },
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        gap = 7.dp,
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = text,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) accentColor else contentColor,
            )
        },
    ) {
        Text(
            text = text,
            color = contentColor,
            style = PicoTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FloatingControlPanel(
    iconRes: Int,
    label: String,
    accentColor: Color,
    badge: String? = null,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onVisualState: (hovered: Boolean, pressed: Boolean) -> Unit,
) {
    val icon = painterResource(iconRes)
    val interactionSource = remember { MutableInteractionSource() }
    val interactionPressed by interactionSource.collectIsPressedAsState()
    var pointerHovered by remember { mutableStateOf(false) }
    var pointerPressed by remember { mutableStateOf(false) }
    val buttonShape = RoundedCornerShape(24.dp)
    val activeColor = when {
        !enabled -> Color(0xFFDADDE2) // design-style: fixed-figma-color linklin cartoon palette
        pointerPressed || interactionPressed -> lerp(
            accentColor,
            Color.Black,
            0.10f,
        )
        pointerHovered -> lerp(
            accentColor,
            Color.White,
            0.10f,
        )
        else -> accentColor
    }
    val iconColor = if (enabled) {
        lerp(accentColor, Color.Black, 0.38f)
    } else {
        Color(0xFF9A9FA6) // design-style: fixed-figma-color linklin cartoon palette
    }

    LaunchedEffect(
        enabled,
        interactionPressed,
        pointerHovered,
        pointerPressed,
    ) {
        if (enabled) {
            onVisualState(
                pointerHovered,
                interactionPressed || pointerPressed,
            )
        } else {
            onVisualState(false, false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.50f
            }
            .clip(buttonShape)
            .pointerInput(enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val inside = event.changes.any { change ->
                            change.position.x >= 0f &&
                                change.position.y >= 0f &&
                                change.position.x <= size.width.toFloat() &&
                                change.position.y <= size.height.toFloat()
                        }
                        val pressed = event.changes.any { it.pressed && inside }
                        if (inside || pointerHovered || pointerPressed) {
                            pointerHovered = inside
                            pointerPressed = pressed
                        }
                    }
                }
            }
            .spatialHoverEffect(enabled = enabled)
            .controllerHapticFeedback(
                type = HandControllerHapticType.Press,
                interactionSource = interactionSource,
            )
            .background(
                color = activeColor,
                shape = buttonShape,
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = icon,
                contentDescription = label,
                modifier = Modifier.size(38.dp),
                tint = iconColor,
            )
            Spacer(Modifier.size(3.dp))
            Text(
                text = label,
                color = Color(0xFF333333), // design-style: fixed-figma-color linklin cartoon palette
                fontSize = 17.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(25.dp)
                    .background(Color.White, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    color = Color(0xFF333333), // design-style: fixed-figma-color linklin cartoon palette
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
