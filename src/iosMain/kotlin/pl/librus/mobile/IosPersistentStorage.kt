package pl.librus.mobile

import platform.Foundation.NSUserDefaults
import pl.librus.mobile.data.storage.PersistentStorage

class IosPersistentStorage : PersistentStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun clear() {
        remove("saved_credentials")
        remove("cached_app_state")
        remove("saved_theme")
    }
}
