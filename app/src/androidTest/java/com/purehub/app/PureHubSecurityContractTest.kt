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

        assertFalse("Android app must remain offline-only", Manifest.permission.INTERNET in permissions)
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
}
