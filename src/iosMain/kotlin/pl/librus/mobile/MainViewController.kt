package pl.librus.mobile

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val storage = remember { IosPersistentStorage() }
    val iconManager = remember { IosAppIconManager(storage) }
    App(storage = storage, iconManager = iconManager)
}
