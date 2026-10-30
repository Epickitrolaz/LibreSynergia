package pl.librus.mobile.platform

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import pl.librus.mobile.LibrusApplication

actual fun showPlatformToast(message: String) {
    val ctx = LibrusApplication.instance ?: return
    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
}

actual fun exitPlatformApp() {
    LibrusApplication.currentActivity?.finish()
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
