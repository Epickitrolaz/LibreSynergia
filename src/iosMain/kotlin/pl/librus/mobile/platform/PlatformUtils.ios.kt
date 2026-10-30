package pl.librus.mobile.platform

import androidx.compose.runtime.Composable

actual fun showPlatformToast(message: String) {
    // iOS has no native short toast; handled via in-app UI when needed.
}

actual fun exitPlatformApp() {
    // iOS guidelines discourage programmatic app exit.
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no hardware back button.
}
