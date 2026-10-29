package pl.librus.mobile

import pl.librus.mobile.data.storage.PersistentStorage
import pl.librus.mobile.ui.icons.AppIcon
import pl.librus.mobile.ui.icons.AppIconManager

class IosAppIconManager(
    private val storage: PersistentStorage,
    private val onIconChange: ((AppIcon) -> Unit)? = null
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
            onIconChange?.invoke(icon)
        } catch (_: Throwable) {
        }
    }

    override fun getCurrentIcon(): AppIcon = current
}
