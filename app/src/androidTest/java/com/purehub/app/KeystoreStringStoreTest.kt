package com.purehub.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.purehub.app.security.KeystoreStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreStringStoreTest {
    @Test
    fun migratesEncryptsAndReplacesValuesOnDevice() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val namespace = "instrumented.secure.store"
        context.getSharedPreferences("$namespace.keystore.v2", android.content.Context.MODE_PRIVATE).edit().clear().commit()

        val store = KeystoreStringStore(context, namespace) { mapOf("legacy" to "old-secret") }
        assertEquals("old-secret", store.getString("legacy"))
        store.putStrings(mapOf("current" to "new-secret"))
        assertEquals("new-secret", store.getString("current"))

        val raw = context.getSharedPreferences("$namespace.keystore.v2", android.content.Context.MODE_PRIVATE)
            .getString("current", "").orEmpty()
        assertFalse(raw.contains("new-secret"))

        store.replace(mapOf("only" to "replacement"))
        assertEquals(null, store.getString("legacy"))
        assertEquals("replacement", store.getString("only"))
    }
}
