package com.kaminova.novalogistikmobile

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.kaminova.novalogistikmobile.data.api.AuthUser
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.data.session.StoredSession
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionUpgradeTest {
    @Test
    fun upgradesAnExistingSessionAndTimeZoneWithoutRestoringItAfterLogout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        fun preferences(name: String) = EncryptedSharedPreferences.create(
            context, name, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val old = preferences("kirimku_secure_prefs")
        val current = preferences("nova_logistik_secure_prefs")
        val oldSession = "kirimku.session.v1"
        val oldZone = "kirimku.evidence.zone"
        val session = "nova_logistik.session.v1"
        val zone = "nova_logistik.evidence.zone"
        val marker = "legacy_preferences_imported"
        fun restoreStrings(pref: SharedPreferences, values: Map<String, String?>) {
            val edit = pref.edit()
            for ((key, value) in values) {
                if (value == null) edit.remove(key) else edit.putString(key, value)
            }
            edit.commit()
        }
        val originalOld = listOf(oldSession, oldZone).associateWith { old.getString(it, null) }
        val originalCurrent = listOf(session, zone).associateWith { current.getString(it, null) }
        val originalMarker = current.getBoolean(marker, false).takeIf { current.contains(marker) }
        val fixture = StoredSession(
            "upgrade-test-token", "TEST", AuthUser(1, "fixture", "Fixture"), "Kirimku",
            Instant.now().plusSeconds(120).toString()
        )
        try {
            assertTrue(current.edit().remove(session).remove(zone).remove(marker).commit())
            assertTrue(old.edit().putString(oldSession, Gson().toJson(fixture)).putString(oldZone, "Asia/Makassar").commit())

            val upgraded = SessionManager(context)
            assertEquals(fixture.token, upgraded.currentToken)
            assertEquals("Asia/Makassar", upgraded.timeZoneState.value)
            assertEquals("Nova Logistik", upgraded.sessionState.value?.companyName)
            assertNull(old.getString(oldSession, null))
            upgraded.clearSession()
            assertNull(SessionManager(context).currentToken)
        } finally {
            restoreStrings(current, originalCurrent)
            restoreStrings(old, originalOld)
            val edit = current.edit()
            if (originalMarker == null) edit.remove(marker) else edit.putBoolean(marker, originalMarker)
            edit.commit()
        }
    }
}
