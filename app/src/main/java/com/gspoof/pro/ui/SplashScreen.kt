package com.gspoof.pro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Splash screen pembuka: "BERSAMA KITA / BISA BISA BISA !"
 * Tampil ~2 detik, teks masuk dengan spring + glow berdenyut, lalu MapScreen.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val scale = remember { Animatable(0.82f) }
    val fade = remember { Animatable(0f) }
    val line = remember { Animatable(0f) }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(500))
        scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 210f))
        line.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(1350)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF04070F),
                        Color(0xFF0C1A31),
                        Color(0xFF07101F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glow di belakang teks
        Box(
            modifier = Modifier
                .size(260.dp)
                .blur(70.dp)
                .alpha(0.35f * pulse)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF17E1C3), Color(0x00000000))
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = fade.value
                }
        ) {
            Text(
                text = "G - S P O O F   P R O",
                color = Color(0xFF3FE0C8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "BERSAMA KITA",
                color = Color(0xFFF3F7FF),
                fontSize = 33.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x883FE0C8),
                        blurRadius = 24f
                    )
                )
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "BISA BISA BISA !",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFFD166),
                            Color(0xFFFF8A5B),
                            Color(0xFF3FE0C8)
                        )
                    ),
                    shadow = Shadow(
                        color = Color(0xAAFFB347),
                        blurRadius = 26f
                    )
                )
            )

            Spacer(Modifier.height(26.dp))

            // Garis progres tipis
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .background(Color(0x22FFFFFF), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .width(160.dp * line.value)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3FE0C8), Color(0xFFFFD166))
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}
