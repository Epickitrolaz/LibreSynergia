package pl.librus.mobile

import pl.librus.mobile.data.storage.PersistentStorage
import pl.librus.mobile.ui.icons.AppIcon
import pl.librus.mobile.ui.icons.AppIconManager
import platform.UIKit.UIApplication

class IosAppIconManager(
    private val storage: PersistentStorage
) : AppIconManager {

    private var current: AppIcon

    init {
        val saved = storage.getString("app_icon_selected")
        current = AppIcon.entries.find { it.id == saved } ?: AppIcon.DEFAULT
    }

    override fun setAppIcon(icon: AppIcon) {
        current = icon
        storage.putString("app_icon_selected", icon.id)
        try {
            val app = UIApplication.sharedApplication
            if (app.supportsAlternateIcons) {
                val alternateName = when (icon) {
                    AppIcon.DEFAULT -> null
                    AppIcon.VARIANT -> "AppIconVariant"
                }
                app.setAlternateIconName(alternateName) { _ -> }
            }
        } catch (_: Throwable) {
        }
    }

    override fun getCurrentIcon(): AppIcon = current
}
