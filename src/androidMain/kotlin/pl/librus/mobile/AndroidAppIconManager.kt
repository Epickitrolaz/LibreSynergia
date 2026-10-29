package pl.librus.mobile

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import pl.librus.mobile.data.storage.PersistentStorage
import pl.librus.mobile.ui.icons.AppIcon
import pl.librus.mobile.ui.icons.AppIconManager

class AndroidAppIconManager(
    private val context: Context,
    private val storage: PersistentStorage
) : AppIconManager {

    private var current: AppIcon
    private var pendingDisable: ComponentName? = null

    init {
        val saved = storage.getString("app_icon_selected")
        current = AppIcon.entries.find { it.id == saved } ?: AppIcon.DEFAULT

        // Register lifecycle listener so disabling the old component happens
        // when the user exits to the home screen, avoiding Android closing the foreground task.
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStopped(activity: Activity) {
                    val compToDisable = pendingDisable ?: return
                    try {
                        context.packageManager.setComponentEnabledSetting(
                            compToDisable,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    } catch (_: Throwable) {
                    }
                    pendingDisable = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
    }

    override fun setAppIcon(icon: AppIcon) {
        if (current == icon && pendingDisable == null) return
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
            // 1. Immediately enable the new icon alias so launcher registers it
            pm.setComponentEnabledSetting(
                enableComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Schedule old alias for disabling when the app is backgrounded
            // Disabling the active alias while in foreground causes Android's ActivityTaskManager
            // to close the running task. Deferring it to onStop avoids the crash entirely.
            pendingDisable = disableComponent
        } catch (_: Throwable) {
        }
    }

    override fun getCurrentIcon(): AppIcon = current
}
