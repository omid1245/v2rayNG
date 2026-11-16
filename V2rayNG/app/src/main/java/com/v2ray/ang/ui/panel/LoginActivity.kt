package com.v2ray.ang.ui.panel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Stage 1: login screen. On success, stores token and imports the panel subscription. */
class LoginActivity : BaseComponentActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }

        /** Persian message for a panel error code. */
        fun errorText(code: Int, error: String): String = when (error) {
            "bad_credentials" -> "نام کاربری یا رمز عبور اشتباه است"
            "device_limit" -> "این حساب روی ۲ دستگاه فعال است و بیشتر مجاز نیست"
            "disabled" -> "این حساب غیرفعال است"
            "too_many" -> "تعداد تلاش‌ها زیاد است؛ کمی بعد دوباره امتحان کنید"
            "panel_unreachable" -> "سرور پنل در دسترس نیست؛ کمی بعد دوباره امتحان کنید"
            "network" -> "اتصال به اینترنت برقرار نیست"
            else -> "خطا در ورود (کد $code)"
        }
    }

    @Composable
    override fun ScreenContent() {
        val store = remember { PanelStore(this) }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ورود به پنل کاربری")
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("نام کاربری") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                singleLine = true,
                enabled = !loading
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("رمز عبور") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                enabled = !loading
            )
            error?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val deviceName = Build.MODEL ?: "Android"
                        val res = withContext(Dispatchers.IO) {
                            PanelApi.login(username.trim(), password, store.deviceId(), deviceName)
                        }
                        when (res) {
                            is PanelResult.Ok -> {
                                store.token = res.data
                                val imported = withContext(Dispatchers.IO) {
                                    val subUrl = (PanelApi.info(res.data) as? PanelResult.Ok)?.data?.subUrl
                                    subUrl != null && PanelRepository.importSubscription(subUrl)
                                }
                                loading = false
                                if (imported) {
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    error = "ورود انجام شد اما دریافت اطلاعات اشتراک ناموفق بود"
                                }
                            }
                            is PanelResult.Err -> {
                                loading = false
                                error = errorText(res.code, res.error)
                            }
                        }
                    }
                },
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                if (loading) CircularProgressIndicator() else Text("ورود")
            }
        }
    }
}
