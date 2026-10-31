package pl.librus.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.librus.mobile.ui.icons.AppIcon
import pl.librus.mobile.ui.icons.AppIconVectorDrawables

/**
 * Renders the official LibreSynergia branding logo matching the user's selected icon preference.
 */
@Composable
fun AppLogo(
    appIcon: AppIcon = AppIcon.DEFAULT,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    shapeRadius: Dp = (size.value * 0.22f).dp
) {
    val vector = when (appIcon) {
        AppIcon.DEFAULT -> AppIconVectorDrawables.DefaultMonogram
        AppIcon.VARIANT -> AppIconVectorDrawables.VariantBook
    }
    val background = when (appIcon) {
        AppIcon.DEFAULT -> Color.Black
        AppIcon.VARIANT -> Color(0xFF141414)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(shapeRadius))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vector,
            contentDescription = "LibreSynergia",
            tint = Color.Unspecified,
            modifier = Modifier.size(size)
        )
    }
}
