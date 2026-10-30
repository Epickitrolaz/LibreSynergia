package pl.librus.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.animation.LocalMotionSpec
import pl.librus.mobile.ui.icons.AppIcon

@Composable
fun AppLaunchScreen(
    isAppReady: Boolean,
    strings: Strings,
    appIcon: AppIcon = AppIcon.DEFAULT,
    onLaunchComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionSpec = LocalMotionSpec.current

    // Entrance animation states
    val emblemScale = remember { Animatable(0.35f) }
    val emblemAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(24f) }

    // Pulsing aura animation during loading
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = motionSpec.duration(1200), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = motionSpec.duration(1200), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Three animated dots at the bottom
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(motionSpec.duration(500), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(motionSpec.duration(500), delayMillis = motionSpec.duration(150), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(motionSpec.duration(500), delayMillis = motionSpec.duration(300), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    LaunchedEffect(Unit) {
        if (!motionSpec.isAnimationsEnabled) {
            onLaunchComplete()
            return@LaunchedEffect
        }

        // Animate emblem in with spring bounce
        emblemAlpha.animateTo(1f, animationSpec = motionSpec.tween(250))
        emblemScale.animateTo(
            targetValue = 1f,
            animationSpec = motionSpec.spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )

        // Animate title and tagline
        contentAlpha.animateTo(1f, animationSpec = motionSpec.tween(300))
        contentOffsetY.animateTo(0f, animationSpec = motionSpec.tween(300))

        // Ensure minimum pleasant duration scaled by animation speed
        val minDisplayMillis = motionSpec.duration(850).toLong()
        delay(minDisplayMillis)

        // Wait until app is ready if still loading
        while (!isAppReady) {
            delay(50)
        }

        onLaunchComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated Emblem Box with Pulsing Aura
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Pulsing ring
                if (motionSpec.isAnimationsEnabled) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                // Core LibreSynergia App Logo Emblem
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(emblemScale.value)
                        .alpha(emblemAlpha.value)
                ) {
                    AppLogo(
                        appIcon = appIcon,
                        size = 80.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Brand Name & Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .offset(y = contentOffsetY.value.dp)
            ) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = strings.launchTagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bottom Loading Dots Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp)
                .alpha(contentAlpha.value)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp)
            ) {
                val dotSize = 8.dp
                val dotColor = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.7f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.launchLoading,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
    }
}
