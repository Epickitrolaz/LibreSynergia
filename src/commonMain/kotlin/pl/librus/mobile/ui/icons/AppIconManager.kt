package pl.librus.mobile.ui.icons

import pl.librus.mobile.data.storage.PersistentStorage

enum class AppIcon(val id: String) {
    DEFAULT("default"),
    VARIANT("variant")
}

interface AppIconManager {
    fun setAppIcon(icon: AppIcon)
    fun getCurrentIcon(): AppIcon
}

class DefaultAppIconManager(
    private val storage: PersistentStorage
) : AppIconManager {
    private var currentIcon: AppIcon = AppIcon.DEFAULT

    init {
        val saved = storage.getString("app_icon_selected")
        if (saved != null) {
            currentIcon = AppIcon.entries.find { it.id == saved } ?: AppIcon.DEFAULT
        }
    }

    override fun setAppIcon(icon: AppIcon) {
        currentIcon = icon
        storage.putString("app_icon_selected", icon.id)
    }

    override fun getCurrentIcon(): AppIcon = currentIcon
}
