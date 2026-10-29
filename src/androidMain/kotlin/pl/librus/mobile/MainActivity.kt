package pl.librus.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val storage = remember { AndroidPersistentStorage(applicationContext) }
            val iconManager = remember { AndroidAppIconManager(applicationContext, storage) }
            App(storage = storage, iconManager = iconManager)
        }
    }
}
