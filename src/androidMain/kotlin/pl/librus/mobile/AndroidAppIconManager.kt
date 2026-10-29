package pl.librus.mobile

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import pl.librus.mobile.data.storage.PersistentStorage
import pl.librus.mobile.ui.icons.AppIcon
import pl.librus.mobile.ui.icons.AppIconManager

class AndroidAppIconManager(
    private val context: Context,
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

        val pm = context.packageManager
        val packageName = context.packageName

        val defaultAlias = ComponentName(packageName, "$packageName.MainActivityDefault")
        val variantAlias = ComponentName(packageName, "$packageName.MainActivityVariant")

        val (enableComponent, disableComponent) = when (icon) {
            AppIcon.DEFAULT -> defaultAlias to variantAlias
            AppIcon.VARIANT -> variantAlias to defaultAlias
        }

        try {
            pm.setComponentEnabledSetting(
                enableComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                disableComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Throwable) {
        }
    }

    override fun getCurrentIcon(): AppIcon = current
}
