package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterStage
import com.example.data.model.Gender
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class SpotLocation(
    val id: Int,
    val normalizedX: Float, // -1f to 1f relative to body center
    val normalizedY: Float, // 0f (head) to 1f (feet)
    val baseRadius: Float,
    val bodyPart: BodyPart
)

enum class BodyPart {
    HEAD, CHEST, LEFT_ARM, RIGHT_ARM, TORSO, LEFT_LEG, RIGHT_LEG
}

/**
 * Precomputes deterministic anatomical spot positions across the avatar body.
 */
fun generateDeterministicSpots(count: Int): List<SpotLocation> {
    val list = mutableListOf<SpotLocation>()
    // Distribution slots:
    // Head/Face: 12%, Chest: 20%, Torso: 25%, Arms: 20%, Legs: 23%
    for (i in 0 until count) {
        val part = when {
            i % 10 in 0..1 -> BodyPart.HEAD
            i % 10 in 2..3 -> BodyPart.CHEST
            i % 10 in 4..5 -> BodyPart.TORSO
            i % 10 == 6 -> BodyPart.LEFT_ARM
            i % 10 == 7 -> BodyPart.RIGHT_ARM
            i % 10 == 8 -> BodyPart.LEFT_LEG
            else -> BodyPart.RIGHT_LEG
        }

        // Pseudo-random deterministic placement based on index
        val angle = (i * 137.5f) * (PI.toFloat() / 180f)
        val radiusFactor = 0.2f + (0.7f * ((i * 31 % 100) / 100f))

        val (nx, ny) = when (part) {
            BodyPart.HEAD -> {
                val x = 0.5f + (cos(angle) * 0.12f * radiusFactor)
                val y = 0.16f + (sin(angle) * 0.08f * radiusFactor)
                x to y
            }
            BodyPart.CHEST -> {
                val x = 0.5f + (cos(angle) * 0.15f * radiusFactor)
                val y = 0.36f + (sin(angle) * 0.06f * radiusFactor)
                x to y
            }
            BodyPart.TORSO -> {
                val x = 0.5f + (cos(angle) * 0.14f * radiusFactor)
                val y = 0.50f + (sin(angle) * 0.08f * radiusFactor)
                x to y
            }
            BodyPart.LEFT_ARM -> {
                val x = 0.25f + ((i % 5) * 0.02f)
                val y = 0.38f + ((i % 7) * 0.03f)
                x to y
            }
            BodyPart.RIGHT_ARM -> {
                val x = 0.75f - ((i % 5) * 0.02f)
                val y = 0.38f + ((i % 7) * 0.03f)
                x to y
            }
            BodyPart.LEFT_LEG -> {
                val x = 0.40f + ((i % 4) * 0.02f)
                val y = 0.70f + ((i % 8) * 0.03f)
                x to y
            }
            BodyPart.RIGHT_LEG -> {
                val x = 0.60f - ((i % 4) * 0.02f)
                val y = 0.70f + ((i % 8) * 0.03f)
                x to y
            }
        }

        val size = 3.6f + ((i * 13 % 4))
        list.add(SpotLocation(i, nx, ny, size, part))
    }
    return list
}

@Composable
fun AnimatedCharacterAvatar(
    gender: Gender,
    stage: CharacterStage,
    initialTotalSpots: Int,
    cleansedSpots: Int,
    modifier: Modifier = Modifier,
    size: Dp = 270.dp
) {
    val totalSpots = initialTotalSpots.coerceIn(30, 100)
    val safeCleansed = cleansedSpots.coerceIn(0, totalSpots)
    val remainingSpotsCount = totalSpots - safeCleansed

    val spots = remember(totalSpots) { generateDeterministicSpots(totalSpots) }

    val infiniteTransition = rememberInfiniteTransition(label = "character_motion")

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (stage) {
            CharacterStage.STAGE_1_TIRED -> 1.01f
            CharacterStage.STAGE_2_HOPEFUL -> 1.025f
            CharacterStage.STAGE_3_HAPPY -> 1.035f
            CharacterStage.STAGE_4_JUMPING -> 1.04f
            CharacterStage.STAGE_5_DANCING -> 1.05f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (stage == CharacterStage.STAGE_1_TIRED) 2200 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Stage 4 Jumping Bounce
    val jumpOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (stage == CharacterStage.STAGE_4_JUMPING) -24f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jump"
    )

    // Stage 5 Dance Sway & Twist
    val danceAngle by infiniteTransition.animateFloat(
        initialValue = if (stage == CharacterStage.STAGE_5_DANCING) -9f else 0f,
        targetValue = if (stage == CharacterStage.STAGE_5_DANCING) 9f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dance_sway"
    )

    // Aura Radiance Pulse
    val auraRadiusPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    // Spot Shimmer/Fade
    val spotPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spot_pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("animated_character_avatar"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = this.size.width
            val canvasH = this.size.height

            val centerX = canvasW / 2f
            val baseY = (canvasH / 2f) + jumpOffsetY

            // 1. Draw Background Aura / Radiance
            drawAura(stage, centerX, baseY, canvasW * 0.45f * auraRadiusPulse)

            // 2. Draw Body base and limbs
            drawCharacterBody(
                stage = stage,
                gender = gender,
                centerX = centerX,
                centerY = baseY,
                scale = breathScale,
                danceAngle = danceAngle,
                width = canvasW,
                height = canvasH
            )

            // 3. Draw Spots (remaining spots are visible, cleansed spots are removed)
            val spotsToDraw = spots.take(remainingSpotsCount)
            for (spot in spotsToDraw) {
                val sx = spot.normalizedX * canvasW
                val sy = (spot.normalizedY * canvasH) + jumpOffsetY

                // Spot shadow and dark core
                drawCircle(
                    color = Color(0x992B1810),
                    radius = spot.baseRadius * spotPulse * 1.2f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color(0xDD4A3528),
                    radius = spot.baseRadius * spotPulse,
                    center = Offset(sx, sy)
                )
                // tiny speck highlight
                drawCircle(
                    color = Color(0x55110B07),
                    radius = spot.baseRadius * 0.5f,
                    center = Offset(sx - 1f, sy - 1f)
                )
            }

            // 4. Draw Stage-specific extras (Crown / Halo / Sparkles for Stage 5)
            if (stage == CharacterStage.STAGE_5_DANCING) {
                drawVictoryCrownAndSparkles(centerX, baseY - (canvasH * 0.38f), canvasW)
            }
        }
    }
}

private fun DrawScope.drawAura(stage: CharacterStage, cx: Float, cy: Float, maxRadius: Float) {
    val auraColors = when (stage) {
        CharacterStage.STAGE_1_TIRED -> listOf(Color(0x22374151), Color(0x00000000))
        CharacterStage.STAGE_2_HOPEFUL -> listOf(Color(0x332DD4BF), Color(0x00000000))
        CharacterStage.STAGE_3_HAPPY -> listOf(Color(0x4410B981), Color(0x00000000))
        CharacterStage.STAGE_4_JUMPING -> listOf(Color(0x6638BDF8), Color(0x2210B981), Color(0x00000000))
        CharacterStage.STAGE_5_DANCING -> listOf(Color(0x88F59E0B), Color(0x5510B981), Color(0x3338BDF8), Color(0x00000000))
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = auraColors,
            center = Offset(cx, cy),
            radius = maxRadius
        ),
        radius = maxRadius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawCharacterBody(
    stage: CharacterStage,
    gender: Gender,
    centerX: Float,
    centerY: Float,
    scale: Float,
    danceAngle: Float,
    width: Float,
    height: Float
) {
    val skinColor = when (stage) {
        CharacterStage.STAGE_1_TIRED -> Color(0xFFC7B89E) // Dull, exhausted tone
        CharacterStage.STAGE_2_HOPEFUL -> Color(0xFFE2C4A6)
        CharacterStage.STAGE_3_HAPPY -> Color(0xFFF3D2B8)
        CharacterStage.STAGE_4_JUMPING -> Color(0xFFFFDAB9)
        CharacterStage.STAGE_5_DANCING -> Color(0xFFFFE0BD) // Glowing radiant skin
    }

    val outfitColor = when (stage) {
        CharacterStage.STAGE_1_TIRED -> Color(0xFF475569)
        CharacterStage.STAGE_2_HOPEFUL -> Color(0xFF0D9488)
        CharacterStage.STAGE_3_HAPPY -> Color(0xFF059669)
        CharacterStage.STAGE_4_JUMPING -> Color(0xFF0284C7)
        CharacterStage.STAGE_5_DANCING -> Color(0xFFD97706)
    }

    val headCenterY = height * 0.17f
    val headRadius = (width * 0.11f) * scale

    // Torso bounds
    val torsoTop = height * 0.28f
    val torsoBottom = height * 0.60f
    val torsoWidth = (width * if (gender == Gender.MALE) 0.28f else 0.25f) * scale

    // Draw Legs
    val legYStart = torsoBottom - 5f
    val legYEnd = height * 0.88f
    val legWidth = width * 0.065f

    val legColor = when (stage) {
        CharacterStage.STAGE_5_DANCING -> Color(0xFF1E293B)
        else -> Color(0xFF334155)
    }

    // Left leg
    val leftLegX = centerX - (torsoWidth * 0.28f)
    val rightLegX = centerX + (torsoWidth * 0.28f)

    if (stage == CharacterStage.STAGE_5_DANCING) {
        // Dynamic dancing leg kick
        drawLine(
            color = legColor,
            start = Offset(leftLegX, legYStart),
            end = Offset(leftLegX - 25f, legYEnd - 10f),
            strokeWidth = legWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = legColor,
            start = Offset(rightLegX, legYStart),
            end = Offset(rightLegX + 20f, legYEnd),
            strokeWidth = legWidth,
            cap = StrokeCap.Round
        )
        // Shoes
        drawCircle(color = outfitColor, radius = legWidth * 0.7f, center = Offset(leftLegX - 25f, legYEnd - 10f))
        drawCircle(color = outfitColor, radius = legWidth * 0.7f, center = Offset(rightLegX + 20f, legYEnd))
    } else {
        // Standing legs
        drawLine(
            color = legColor,
            start = Offset(leftLegX, legYStart),
            end = Offset(leftLegX, legYEnd),
            strokeWidth = legWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = legColor,
            start = Offset(rightLegX, legYStart),
            end = Offset(rightLegX, legYEnd),
            strokeWidth = legWidth,
            cap = StrokeCap.Round
        )
        // Shoes
        drawCircle(color = outfitColor, radius = legWidth * 0.7f, center = Offset(leftLegX, legYEnd))
        drawCircle(color = outfitColor, radius = legWidth * 0.7f, center = Offset(rightLegX, legYEnd))
    }

    // Draw Torso
    val torsoPath = Path().apply {
        moveTo(centerX - (torsoWidth / 2f), torsoTop)
        lineTo(centerX + (torsoWidth / 2f), torsoTop)
        lineTo(centerX + (torsoWidth * 0.42f), torsoBottom)
        lineTo(centerX - (torsoWidth * 0.42f), torsoBottom)
        close()
    }
    drawPath(path = torsoPath, color = outfitColor)

    // Chest Collar Accent
    drawCircle(
        color = skinColor,
        radius = headRadius * 0.4f,
        center = Offset(centerX, torsoTop + 2f)
    )

    // Draw Arms
    val armWidth = width * 0.055f
    val shoulderY = torsoTop + 10f

    when (stage) {
        CharacterStage.STAGE_1_TIRED -> {
            // Drooping, exhausted arms hanging down heavily
            drawLine(
                color = skinColor,
                start = Offset(centerX - (torsoWidth / 2f), shoulderY),
                end = Offset(centerX - (torsoWidth * 0.70f), torsoBottom + 20f),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = skinColor,
                start = Offset(centerX + (torsoWidth / 2f), shoulderY),
                end = Offset(centerX + (torsoWidth * 0.70f), torsoBottom + 20f),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
        }
        CharacterStage.STAGE_2_HOPEFUL -> {
            // Relaxed arms
            drawLine(
                color = skinColor,
                start = Offset(centerX - (torsoWidth / 2f), shoulderY),
                end = Offset(centerX - (torsoWidth * 0.65f), torsoBottom),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = skinColor,
                start = Offset(centerX + (torsoWidth / 2f), shoulderY),
                end = Offset(centerX + (torsoWidth * 0.65f), torsoBottom),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
        }
        CharacterStage.STAGE_3_HAPPY -> {
            // Confident arms on waist / slightly forward
            drawLine(
                color = skinColor,
                start = Offset(centerX - (torsoWidth / 2f), shoulderY),
                end = Offset(centerX - (torsoWidth * 0.75f), torsoTop + ((torsoBottom - torsoTop) * 0.6f)),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = skinColor,
                start = Offset(centerX + (torsoWidth / 2f), shoulderY),
                end = Offset(centerX + (torsoWidth * 0.75f), torsoTop + ((torsoBottom - torsoTop) * 0.6f)),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
        }
        CharacterStage.STAGE_4_JUMPING -> {
            // Arms raised up high in celebration V-shape!
            drawLine(
                color = skinColor,
                start = Offset(centerX - (torsoWidth / 2f), shoulderY),
                end = Offset(centerX - (torsoWidth * 0.95f), headCenterY - 15f),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = skinColor,
                start = Offset(centerX + (torsoWidth / 2f), shoulderY),
                end = Offset(centerX + (torsoWidth * 0.95f), headCenterY - 15f),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
        }
        CharacterStage.STAGE_5_DANCING -> {
            // Dynamic dancing wavy arms with celebration hands!
            val waveOffset = sin((danceAngle * PI / 180f).toFloat()) * 20f
            drawLine(
                color = skinColor,
                start = Offset(centerX - (torsoWidth / 2f), shoulderY),
                end = Offset(centerX - (torsoWidth * 0.90f), headCenterY + waveOffset),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = skinColor,
                start = Offset(centerX + (torsoWidth / 2f), shoulderY),
                end = Offset(centerX + (torsoWidth * 0.90f), headCenterY - waveOffset - 25f),
                strokeWidth = armWidth,
                cap = StrokeCap.Round
            )
        }
    }

    // Draw Neck
    drawLine(
        color = skinColor,
        start = Offset(centerX, torsoTop),
        end = Offset(centerX, headCenterY + (headRadius * 0.7f)),
        strokeWidth = width * 0.08f,
        cap = StrokeCap.Round
    )

    // Draw Head
    drawCircle(
        color = skinColor,
        radius = headRadius,
        center = Offset(centerX, headCenterY)
    )

    // Draw Hair / Beard by Gender
    drawHairAndStyling(gender, stage, centerX, headCenterY, headRadius)

    // Draw Facial Expression (Eyes, Eyebrows, Mouth, Sweat/Tears or Joy Blush)
    drawFacialFeatures(stage, centerX, headCenterY, headRadius)
}

private fun DrawScope.drawHairAndStyling(
    gender: Gender,
    stage: CharacterStage,
    cx: Float,
    cy: Float,
    r: Float
) {
    val hairColor = when (stage) {
        CharacterStage.STAGE_1_TIRED -> Color(0xFF4A403A)
        else -> Color(0xFF2C1D11)
    }

    if (gender == Gender.MALE) {
        // Male clean modern crop hairstyle
        val hairPath = Path().apply {
            moveTo(cx - (r * 0.95f), cy - (r * 0.1f))
            cubicTo(
                cx - (r * 0.8f), cy - (r * 1.25f),
                cx + (r * 0.8f), cy - (r * 1.25f),
                cx + (r * 0.95f), cy - (r * 0.1f)
            )
            lineTo(cx + (r * 0.85f), cy - (r * 0.4f))
            cubicTo(
                cx + (r * 0.4f), cy - (r * 0.8f),
                cx - (r * 0.4f), cy - (r * 0.8f),
                cx - (r * 0.85f), cy - (r * 0.4f)
            )
            close()
        }
        drawPath(path = hairPath, color = hairColor)

        // Neat beard trim
        val beardColor = hairColor.copy(alpha = 0.85f)
        drawArc(
            color = beardColor,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - (r * 0.82f), cy - (r * 0.2f)),
            size = Size(r * 1.64f, r * 1.2f),
            style = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
        )
    } else {
        // Female long wavy flowing hair
        val hairPath = Path().apply {
            moveTo(cx - (r * 1.1f), cy + (r * 0.8f))
            lineTo(cx - (r * 0.95f), cy - (r * 0.3f))
            cubicTo(
                cx - (r * 0.9f), cy - (r * 1.3f),
                cx + (r * 0.9f), cy - (r * 1.3f),
                cx + (r * 0.95f), cy - (r * 0.3f)
            )
            lineTo(cx + (r * 1.1f), cy + (r * 0.8f))
            cubicTo(
                cx + (r * 0.9f), cy + (r * 0.3f),
                cx + (r * 0.7f), cy - (r * 0.5f),
                cx, cy - (r * 0.6f)
            )
            cubicTo(
                cx - (r * 0.7f), cy - (r * 0.5f),
                cx - (r * 0.9f), cy + (r * 0.3f),
                cx - (r * 1.1f), cy + (r * 0.8f)
            )
            close()
        }
        drawPath(path = hairPath, color = hairColor)
    }
}

private fun DrawScope.drawFacialFeatures(
    stage: CharacterStage,
    cx: Float,
    cy: Float,
    r: Float
) {
    val eyeOffsetX = r * 0.35f
    val eyeOffsetY = cy - (r * 0.05f)
    val eyeColor = Color(0xFF1E293B)

    when (stage) {
        CharacterStage.STAGE_1_TIRED -> {
            // Tired, sad, heavy drooping eyes (- -)
            drawLine(
                color = eyeColor,
                start = Offset(cx - eyeOffsetX - (r * 0.15f), eyeOffsetY + 2f),
                end = Offset(cx - eyeOffsetX + (r * 0.15f), eyeOffsetY + 6f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = eyeColor,
                start = Offset(cx + eyeOffsetX - (r * 0.15f), eyeOffsetY + 6f),
                end = Offset(cx + eyeOffsetX + (r * 0.15f), eyeOffsetY + 2f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )

            // Sad turned-down curved mouth ☹
            drawArc(
                color = eyeColor,
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(cx - (r * 0.3f), cy + (r * 0.28f)),
                size = Size(r * 0.6f, r * 0.4f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // Exhaustion sweat drop
            drawCircle(color = Color(0x9938BDF8), radius = r * 0.1f, center = Offset(cx + (r * 0.75f), cy + (r * 0.1f)))
        }
        CharacterStage.STAGE_2_HOPEFUL -> {
            // Calm open eyes
            drawCircle(color = eyeColor, radius = r * 0.09f, center = Offset(cx - eyeOffsetX, eyeOffsetY))
            drawCircle(color = eyeColor, radius = r * 0.09f, center = Offset(cx + eyeOffsetX, eyeOffsetY))

            // Gentle slight smile
            drawArc(
                color = eyeColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - (r * 0.25f), cy + (r * 0.18f)),
                size = Size(r * 0.5f, r * 0.3f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
        }
        CharacterStage.STAGE_3_HAPPY -> {
            // Sparkling happy eyes with white reflections
            drawCircle(color = eyeColor, radius = r * 0.11f, center = Offset(cx - eyeOffsetX, eyeOffsetY))
            drawCircle(color = eyeColor, radius = r * 0.11f, center = Offset(cx + eyeOffsetX, eyeOffsetY))
            drawCircle(color = Color.White, radius = r * 0.04f, center = Offset(cx - eyeOffsetX - 2f, eyeOffsetY - 2f))
            drawCircle(color = Color.White, radius = r * 0.04f, center = Offset(cx + eyeOffsetX - 2f, eyeOffsetY - 2f))

            // Warm smile
            drawArc(
                color = eyeColor,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(cx - (r * 0.32f), cy + (r * 0.12f)),
                size = Size(r * 0.64f, r * 0.45f),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // Rosy cheeks
            drawCircle(color = Color(0x44FB7185), radius = r * 0.16f, center = Offset(cx - (r * 0.55f), cy + (r * 0.18f)))
            drawCircle(color = Color(0x44FB7185), radius = r * 0.16f, center = Offset(cx + (r * 0.55f), cy + (r * 0.18f)))
        }
        CharacterStage.STAGE_4_JUMPING, CharacterStage.STAGE_5_DANCING -> {
            // Joyful curved laughing eyes (^ ^)
            drawArc(
                color = eyeColor,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - eyeOffsetX - (r * 0.18f), eyeOffsetY - 8f),
                size = Size(r * 0.36f, r * 0.3f),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx + eyeOffsetX - (r * 0.18f), eyeOffsetY - 8f),
                size = Size(r * 0.36f, r * 0.3f),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )

            // Wide ecstatic open smile with tongue
            val mouthPath = Path().apply {
                moveTo(cx - (r * 0.35f), cy + (r * 0.12f))
                quadraticTo(cx, cy + (r * 0.55f), cx + (r * 0.35f), cy + (r * 0.12f))
                close()
            }
            drawPath(path = mouthPath, color = Color(0xFFDC2626))
            drawPath(path = mouthPath, color = eyeColor, style = Stroke(width = 3.5f))

            // Rosy glowing cheeks
            drawCircle(color = Color(0x66FB7185), radius = r * 0.20f, center = Offset(cx - (r * 0.55f), cy + (r * 0.15f)))
            drawCircle(color = Color(0x66FB7185), radius = r * 0.20f, center = Offset(cx + (r * 0.55f), cy + (r * 0.15f)))
        }
    }
}

private fun DrawScope.drawVictoryCrownAndSparkles(cx: Float, topY: Float, width: Float) {
    // Golden Crown of Victory
    val crownWidth = width * 0.26f
    val crownHeight = width * 0.14f

    val crownPath = Path().apply {
        moveTo(cx - (crownWidth / 2f), topY)
        lineTo(cx - (crownWidth * 0.6f), topY - crownHeight)
        lineTo(cx - (crownWidth * 0.2f), topY - (crownHeight * 0.4f))
        lineTo(cx, topY - (crownHeight * 1.25f))
        lineTo(cx + (crownWidth * 0.2f), topY - (crownHeight * 0.4f))
        lineTo(cx + (crownWidth * 0.6f), topY - crownHeight)
        lineTo(cx + (crownWidth / 2f), topY)
        close()
    }
    drawPath(path = crownPath, color = Color(0xFFFFD700))
    drawPath(path = crownPath, color = Color(0xFFB45309), style = Stroke(width = 3f))

    // Crown jewels
    drawCircle(color = Color(0xFFE11D48), radius = 4.5f, center = Offset(cx, topY - (crownHeight * 1.25f) + 2f))
    drawCircle(color = Color(0xFF0284C7), radius = 3.5f, center = Offset(cx - (crownWidth * 0.6f), topY - crownHeight + 2f))
    drawCircle(color = Color(0xFF059669), radius = 3.5f, center = Offset(cx + (crownWidth * 0.6f), topY - crownHeight + 2f))

    // Sparkle stars
    drawStarSparkle(cx - (width * 0.35f), topY - 10f, 10f, Color(0xFFFFD700))
    drawStarSparkle(cx + (width * 0.35f), topY - 15f, 12f, Color(0xFF38BDF8))
    drawStarSparkle(cx - (width * 0.25f), topY - 40f, 8f, Color(0xFF10B981))
    drawStarSparkle(cx + (width * 0.25f), topY - 45f, 9f, Color(0xFFF43F5E))
}

private fun DrawScope.drawStarSparkle(x: Float, y: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, y - size)
        quadraticTo(x, y, x + size, y)
        quadraticTo(x, y, x, y + size)
        quadraticTo(x, y, x - size, y)
        quadraticTo(x, y, x, y - size)
        close()
    }
    drawPath(path = path, color = color)
}
