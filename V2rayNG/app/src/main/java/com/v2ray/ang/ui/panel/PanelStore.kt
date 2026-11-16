package com.v2ray.ang.ui.panel

import android.content.Context
import java.util.UUID

/**
 * Stores token and device id. TODO: move token to encrypted storage (EncryptedSharedPreferences)
 * in a later stage; kept simple here.
 */
class PanelStore(context: Context) {
    private val prefs = context.getSharedPreferences("panel_store", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    val isLoggedIn: Boolean get() = !token.isNullOrBlank()

    /** Stable random id, generated once per install. */
    fun deviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_DEVICE_ID = "device_id"
    }
}
