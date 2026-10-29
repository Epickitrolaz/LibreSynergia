package pl.librus.mobile

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(
    onIconChange: ((String) -> Unit)? = null
): UIViewController = ComposeUIViewController {
    val storage = remember { IosPersistentStorage() }
    val iconManager = remember {
        IosAppIconManager(storage) { icon ->
            onIconChange?.invoke(icon.id)
        }
    }
    App(storage = storage, iconManager = iconManager)
}
