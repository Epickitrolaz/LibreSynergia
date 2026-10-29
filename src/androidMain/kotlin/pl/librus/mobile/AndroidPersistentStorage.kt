package pl.librus.mobile

import android.content.Context
import pl.librus.mobile.data.storage.PersistentStorage

class AndroidPersistentStorage(context: Context) : PersistentStorage {
    private val prefs = context.getSharedPreferences("librus_mobile_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
