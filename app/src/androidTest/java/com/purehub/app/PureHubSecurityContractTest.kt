package com.purehub.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PureHubSecurityContractTest {
    @Test
    fun packagedAppIsOfflineAndDeclaresHardwarePermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions.orEmpty().toSet()

        if (BuildConfig.INTERNET_ENABLED) {
            assertTrue("Connected flavor must declare INTERNET for explicit workflows", Manifest.permission.INTERNET in permissions)
        } else {
            assertFalse("F-Droid flavor must remain offline-only", Manifest.permission.INTERNET in permissions)
        }
        assertTrue(Manifest.permission.CAMERA in permissions)
        assertTrue(Manifest.permission.RECORD_AUDIO in permissions)
        assertTrue(Manifest.permission.ACCESS_WIFI_STATE in permissions)
        assertTrue(Manifest.permission.SET_WALLPAPER in permissions)
        assertFalse(
            "Sensitive local data must not enter Android cloud backup",
            context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0,
        )
    }

    @Test
    fun cameraAndMicrophoneRemainOptionalHardware() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val features = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_CONFIGURATIONS,
        ).reqFeatures.orEmpty().associate { it.name to it.flags }

        val requiredFlag = android.content.pm.FeatureInfo.FLAG_REQUIRED
        assertFalse((features[PackageManager.FEATURE_CAMERA] ?: 0) and requiredFlag != 0)
        assertFalse((features[PackageManager.FEATURE_MICROPHONE] ?: 0) and requiredFlag != 0)
    }

    @Test
    fun onlyLauncherActivityIsPubliclyExported() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS,
        )

        // Compose's debug/test manifests add exported tooling activities. They
        // are not packaged in release, so enforce the contract on PureHub-owned
        // components while still checking every provider below.
        val appNamespace = MainActivity::class.java.packageName + "."
        val exportedActivities = packageInfo.activities.orEmpty()
            .filter { it.exported && it.name.startsWith(appNamespace) }
            .map { it.name }
        assertTrue("Only MainActivity may be exported: $exportedActivities", exportedActivities == listOf(MainActivity::class.java.name))
        assertTrue("Services must remain app-private", packageInfo.services.orEmpty().none { it.exported && it.name.startsWith(appNamespace) })
        assertTrue("Widget and internal receivers must remain app-private", packageInfo.receivers.orEmpty().none { it.exported && it.name.startsWith(appNamespace) })
        assertTrue("File and database providers must remain app-private", packageInfo.providers.orEmpty().none { it.exported })
    }
}
