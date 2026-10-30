package pl.librus.mobile.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pl.librus.mobile.ui.animation.LocalMotionSpec

@Composable
fun AnimatedDialogContainer(
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val motionSpec = LocalMotionSpec.current

    AnimatedVisibility(
        visible = visible,
        enter = if (motionSpec.isAnimationsEnabled) {
            fadeIn(animationSpec = motionSpec.tween(220)) +
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = motionSpec.spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        } else {
            EnterTransition.None
        },
        exit = if (motionSpec.isAnimationsEnabled) {
            fadeOut(animationSpec = motionSpec.tween(180)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = motionSpec.tween(180)
                )
        } else {
            ExitTransition.None
        },
        modifier = modifier
    ) {
        content()
    }
}
