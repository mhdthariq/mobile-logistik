package com.kaminova.novalogistikmobile.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.kaminova.novalogistikmobile.data.api.AuthUser
import com.kaminova.novalogistikmobile.util.EvidenceZones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

data class StoredSession(
    val token: String,
    val corpId: String?,
    val user: AuthUser,
    val companyName: String,
    val expiresAt: String
) {
    val isExpired: Boolean
        get() = runCatching {
            Instant.parse(expiresAt).isBefore(Instant.now())
        }.getOrDefault(false)
}

class SessionManager(context: Context) {
    private val gson = Gson()
    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        fun encrypted(name: String) = EncryptedSharedPreferences.create(
            context, name, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        encrypted("nova_logistik_secure_prefs").also { target ->
            migratePreferences(context, target, "${LEGACY_NAMESPACE}_secure_prefs", ::encrypted)
        }
    }.getOrElse {
        context.getSharedPreferences("nova_logistik_fallback_prefs", Context.MODE_PRIVATE).also { target ->
            migratePreferences(context, target, "${LEGACY_NAMESPACE}_fallback_prefs") { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
            }
        }
    }

    private fun migratePreferences(
        context: Context,
        target: SharedPreferences,
        oldName: String,
        open: (String) -> SharedPreferences
    ) {
        if (target.getBoolean(KEY_MIGRATED, false) ||
            !context.dataDir.resolve("shared_prefs/$oldName.xml").isFile) return

        runCatching {
            val source = open(oldName)
            val sessionKey = "$LEGACY_NAMESPACE.session.v1"
            val zoneKey = "$LEGACY_NAMESPACE.evidence.zone"
            val edit = target.edit().putBoolean(KEY_MIGRATED, true)
            if (!target.contains(KEY_SESSION)) source.getString(sessionKey, null)?.let { edit.putString(KEY_SESSION, it) }
            if (!target.contains(KEY_TIME_ZONE)) source.getString(zoneKey, null)?.let { edit.putString(KEY_TIME_ZONE, it) }
            if (edit.commit()) source.edit().remove(sessionKey).remove(zoneKey).apply()
        }
    }

    private val _sessionState = MutableStateFlow<StoredSession?>(null)
    val sessionState: StateFlow<StoredSession?> = _sessionState.asStateFlow()

    private val _timeZoneState = MutableStateFlow(EvidenceZones.DEFAULT_ZONE_ID)
    val timeZoneState: StateFlow<String> = _timeZoneState.asStateFlow()

    private val _usePhoneColors = MutableStateFlow(prefs.getBoolean(KEY_PHONE_COLORS, false))
    val usePhoneColors: StateFlow<Boolean> = _usePhoneColors.asStateFlow()

    fun setUsePhoneColors(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PHONE_COLORS, enabled).apply()
        _usePhoneColors.value = enabled
    }

    init {
        loadSession()
        loadTimeZone()
    }

    fun loadSession(): StoredSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: run {
            _sessionState.value = null
            return null
        }
        val session = runCatching {
            gson.fromJson(raw, StoredSession::class.java)
        }.getOrNull()

        if (session == null || session.token.isBlank() || session.isExpired) {
            clearSession()
            return null
        }

        if (session.companyName.equals(LEGACY_NAMESPACE, ignoreCase = true)) {
            return session.copy(companyName = "Nova Logistik").also(::saveSession)
        }
        _sessionState.value = session
        return session
    }

    fun saveSession(session: StoredSession) {
        prefs.edit()
            .putString(KEY_SESSION, gson.toJson(session))
            .apply()
        _sessionState.value = session
    }

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION).apply()
        _sessionState.value = null
    }

    fun loadTimeZone(): String {
        val zone = prefs.getString(KEY_TIME_ZONE, EvidenceZones.DEFAULT_ZONE_ID)
            ?: EvidenceZones.DEFAULT_ZONE_ID
        _timeZoneState.value = zone
        return zone
    }

    fun saveTimeZone(zone: String) {
        prefs.edit().putString(KEY_TIME_ZONE, zone).apply()
        _timeZoneState.value = zone
    }

    val currentToken: String?
        get() = _sessionState.value?.token

    val currentCorpId: String?
        get() = _sessionState.value?.corpId

    companion object {
        private const val KEY_SESSION = "nova_logistik.session.v1"
        private const val KEY_TIME_ZONE = "nova_logistik.evidence.zone"
        private const val KEY_PHONE_COLORS = "nova_logistik.theme.phone_colors"
        private const val KEY_MIGRATED = "legacy_preferences_imported"
        // Needed to read settings from older installations.
        private const val LEGACY_NAMESPACE = "kirimku"
    }
}
