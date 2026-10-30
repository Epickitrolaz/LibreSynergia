package pl.librus.mobile.platform

import androidx.compose.runtime.Composable

expect fun showPlatformToast(message: String)

expect fun exitPlatformApp()

@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
