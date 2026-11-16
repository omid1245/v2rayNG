package com.v2ray.ang.ui.panel

import com.v2ray.ang.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Result of a call to the panel API. */
sealed class PanelResult<out T> {
    data class Ok<T>(val data: T) : PanelResult<T>()
    data class Err(val code: Int, val error: String) : PanelResult<Nothing>()
}

data class PanelInfo(
    val username: String,
    val subUrl: String,
    val used: Long,
    val total: Long,
    val expiryMs: Long,
    val enable: Boolean,
    val devices: Int,
    val maxDevices: Int,
)

/** Thin client for the panel API described in PROMPT.md. Never logs the sub_url or token. */
object PanelApi {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): String = BuildConfig.API_URL.trimEnd('/')

    /** POST /api/login -> token */
    fun login(username: String, password: String, deviceId: String, deviceName: String): PanelResult<String> {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("device_id", deviceId)
            .put("device_name", deviceName)
            .toString()
            .toRequestBody(json)
        return execute(Request.Builder().url("${baseUrl()}/api/login").post(body).build()) { obj ->
            obj.getString("token")
        }
    }

    /** GET /api/info */
    fun info(token: String): PanelResult<PanelInfo> {
        val req = Request.Builder()
            .url("${baseUrl()}/api/info")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return execute(req) { o ->
            PanelInfo(
                username = o.optString("username"),
                subUrl = o.optString("sub_url"),
                used = o.optLong("used"),
                total = o.optLong("total"),
                expiryMs = o.optLong("expiry_ms"),
                enable = o.optBoolean("enable", true),
                devices = o.optInt("devices"),
                maxDevices = o.optInt("max_devices"),
            )
        }
    }

    /** POST /api/logout (best effort, errors are ignored by the caller). */
    fun logout(token: String): PanelResult<Unit> {
        val req = Request.Builder()
            .url("${baseUrl()}/api/logout")
            .header("Authorization", "Bearer $token")
            .post("{}".toRequestBody(json))
            .build()
        return execute(req) { }
    }

    private inline fun <T> execute(request: Request, parse: (JSONObject) -> T): PanelResult<T> {
        return try {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val err = try { JSONObject(text).optString("error", "http_${resp.code}") } catch (_: Exception) { "http_${resp.code}" }
                    return PanelResult.Err(resp.code, err)
                }
                val obj = if (text.isBlank()) JSONObject() else JSONObject(text)
                PanelResult.Ok(parse(obj))
            }
        } catch (_: Exception) {
            PanelResult.Err(0, "network")
        }
    }
}
