package com.purehub.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.purehub.app.ui.PureHubApp
import com.purehub.app.ui.theme.PureHubTheme
import com.purehub.app.feature.catalog.MiniAppId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installMiniAppShortcuts()
        val initialMiniAppId = intent.getStringExtra(EXTRA_MINI_APP_ID)?.let { value ->
            MiniAppId.entries.firstOrNull { it.name == value }
        }

        setContent {
            PureHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PureHubApp(initialMiniAppId = initialMiniAppId)
                }
            }
        }
    }

    private fun installMiniAppShortcuts() {
        val shortcuts = listOf(
            Triple("qr_studio", "QR Studio", MiniAppId.QR_STUDIO),
            Triple("zen_pomodoro", "Zen Pomodoro", MiniAppId.ZEN_POMODORO),
            Triple("zen_breath", "Zen Breath", MiniAppId.ZEN_BREATH),
        ).map { (id, label, miniAppId) ->
            ShortcutInfoCompat.Builder(this, id)
                .setShortLabel(label)
                .setLongLabel("Open $label in PureHub")
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(EXTRA_MINI_APP_ID, miniAppId.name))
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(this, shortcuts)
    }

    companion object {
        private const val EXTRA_MINI_APP_ID = "purehub.mini_app_id"
    }
}
