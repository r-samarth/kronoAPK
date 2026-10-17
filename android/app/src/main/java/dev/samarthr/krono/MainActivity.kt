package dev.samarthr.krono

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var store: TaskStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The app remains usable even if notifications are declined. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF0D0E11.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF0D0E11.toInt())
        )

        store = TaskStore(applicationContext)
        requestNotificationPermissionIfNeeded()

        setContent {
            KronoAndroidScreen(store = store)
        }
    }

    override fun onDestroy() {
        if (::store.isInitialized) {
            store.close()
        }
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val isGranted = ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            notificationPermissionLauncher.launch(permission)
        }
    }
}
