package com.linklink.myspatial

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.linklink.myspatial.content.HomeVolume
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.systemColorScheme
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.content.panelSize
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.linklink.myspatial.content.AppLanguage
import kotlin.random.Random

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultStage {
            LinklinTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // The screen-space veil can occlude the composited 3D Stage
                    // on emulator recreation. The 3D cosmos cube is authoritative.
                    // StageCosmosBackdrop()
                    // PICO hides a Full Stage whose Android surface has no visible
                    // pixels. A near-transparent 2dp anchor keeps the Stage alive
                    // while covering too little of the frame to hide direct 3D ECS.
                    StageKeepAliveAnchor()
                    StageKeepAliveRing()
                    var appLanguage by remember { mutableStateOf(AppLanguage.CHINESE) }
                    val baseContext = LocalContext.current
                    val localizedContext = remember(appLanguage, baseContext) {
                        val configuration = Configuration(baseContext.resources.configuration)
                        configuration.setLocale(appLanguage.locale)
                        baseContext.createConfigurationContext(configuration)
                    }
                    CompositionLocalProvider(LocalContext provides localizedContext) {
                        HomeVolume(
                            appLanguage = appLanguage,
                            onToggleLanguage = {
                                appLanguage = if (appLanguage == AppLanguage.CHINESE) {
                                    AppLanguage.ENGLISH
                                } else {
                                    AppLanguage.CHINESE
                                }
                            },
                        )
                    }
                }
            }
        }
    }

/**
 * A screen-space safety layer. The 3D star enclosure provides depth and parallax,
 * while this opaque Compose layer guarantees that viewport zoom/dolly changes can
 * never reveal the PICO system environment behind the Stage.
 */
@Composable
private fun StageCosmosBackdrop() {
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Keep a first-frame surface for the Stage, but let the 3D cosmos and
        // AttachmentPanel layers composite through this safety veil.
        drawRect(color = Color.Black.copy(alpha = 0.01f))
        STAGE_STAR_SPECS.forEach { star ->
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = size.minDimension * star.radius,
                center = Offset(
                    x = size.width * star.x,
                    y = size.height * star.y,
                ),
            )
        }
    }
}

@Composable
private fun StageKeepAliveAnchor() {
    Canvas(modifier = Modifier.size(2.dp)) {
        drawRect(color = Color.Black.copy(alpha = 0.02f))
    }
}

/**
 * PICO can hide a Full Stage when its Android surface has no visible pixels in
 * the current view. One tiny forward anchor disappears from the compositor when
 * the player turns around. Six nearly invisible attachment anchors form a 360°
 * keep-alive ring while the direct-3D cosmos sphere remains the visible sky.
 */
@Composable
private fun StageKeepAliveRing() {
    val anchorSize = panelSize(
        width = 0.002f,
        height = 0.002f,
        lengthUnit = LengthUnit.Meters,
    )

    SpatialView(
        initial = { content, attachments ->
            // Twelve anchors leave no orientation gap in the headset FOV. They
            // remain physically tiny and nearly transparent, so only the 3D
            // cosmos sphere is visible.
            repeat(12) { index ->
                val yaw = index * 30f
                val radians = Math.toRadians(yaw.toDouble())
                val position = Vector3(
                    x = (0.55 * kotlin.math.sin(radians)).toFloat(),
                    y = 1.55f,
                    z = (0.55 * kotlin.math.cos(radians)).toFloat(),
                )
                attachments.entity(id = "stage_keepalive_$index")?.apply {
                    components.set(
                        com.pico.spatial.core.ecs.TransformComponent(),
                    )
                    components[com.pico.spatial.core.ecs.TransformComponent::class.java]?.apply {
                        setPosition(position)
                        setEulerAngles(EulerAngles(yaw = yaw))
                    }
                    content.addEntity(this)
                }
            }
        },
        attachments = {
            repeat(6) { index ->
                AttachmentPanel(
                    id = "stage_keepalive_$index",
                    size = anchorSize,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color.Black.copy(alpha = 0.02f))
                    }
                }
            }
        },
    )
}

private data class StageStarSpec(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)

private val STAGE_STAR_SPECS: List<StageStarSpec> = run {
    val random = Random(20260920L)
    List(520) {
        StageStarSpec(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radius = when {
                random.nextFloat() < 0.08f -> 0.0022f + random.nextFloat() * 0.0016f
                random.nextFloat() < 0.34f -> 0.0013f + random.nextFloat() * 0.0009f
                else -> 0.0006f + random.nextFloat() * 0.0006f
            },
            alpha = 0.34f + random.nextFloat() * 0.58f,
        )
    }
}

@Composable
private fun LinklinTheme(content: @Composable () -> Unit) {
    val system = systemColorScheme(LocalContext.current)
    PicoTheme(
        colorScheme = system.copy(
            fillPrimary = Color(0xFFFFB347), // design-style: fixed-figma-color linklin cartoon palette
            fillSecondary = Color(0xFF8ACBFF), // design-style: fixed-figma-color linklin cartoon palette
            fillTertiary = Color(0xFF9EE8C8), // design-style: fixed-figma-color linklin cartoon palette
            fillLight = Color(0xCCFFFFFF), // design-style: fixed-figma-color linklin cartoon palette
            labelPrimaryLight = Color(0xFFFFFFFF), // design-style: fixed-figma-color linklin cartoon palette
            labelPrimary = Color(0xFF333333), // design-style: fixed-figma-color linklin cartoon palette
            labelSecondary = Color(0xFF777777), // design-style: fixed-figma-color linklin cartoon palette
            labelTertiary = Color(0xFF8A9099), // design-style: fixed-figma-color linklin cartoon palette
            labelQuaternary = Color(0xFFE1E4E8), // design-style: fixed-figma-color linklin cartoon palette
            lightenHover = Color(0xFFFFC678), // design-style: fixed-figma-color linklin cartoon palette
            lightenPressed = Color(0xFFE69A35), // design-style: fixed-figma-color linklin cartoon palette
            error = Color(0xFFFF9E9E), // design-style: fixed-figma-color linklin cartoon palette
            alert = Color(0xFFF08A00), // design-style: fixed-figma-color linklin cartoon palette
            passable = Color(0xFF9EE8C8), // design-style: fixed-figma-color linklin cartoon palette
            interaction = Color(0xFF8ACBFF), // design-style: fixed-figma-color linklin cartoon palette
            dividerLine = Color(0xFFE1E4E8), // design-style: fixed-figma-color linklin cartoon palette
        ),
    ) {
        content()
    }
}
